# Apache Spark - Apache HBase Connector

The [Apache Spark](https://spark.apache.org/) - [Apache HBase](https://hbase.apache.org/) Connector is a library to support Spark accessing HBase table as external data source or sink. With it, user can operate HBase with Spark-SQL on data frame level. 

With the data frame support, the lib leverages all the optimization techniques in catalyst, and achieves data locality, partition pruning, predicate pushdown, Scanning and BulkGet, etc. 

## Catalog
For each table, a catalog has to be provided,  which includes the row key, and the columns with data type with predefined column families, and defines the mapping between hbase column and table schema. The catalog is user defined json format.

## Datatype conversion
Java primitive types is supported. In the future, other data types will be supported, which relies on user specified sedes. Take avro as an example. User defined sedes will be responsible to convert byte array to avro object, and connector will be responsible to convert avro object to catalyst supported datatypes. 

Note that if user want dataframe to only handle byte array, the binary type can be specified. Then user can get the catalyst row with each column as a byte array. User can further deserialize it with customized deserializer, or operate on the RDD of the data frame directly.

## Solve the conflicts of Catalyst datatype order and HBase byte array order
The libary automatically handle the orderness between the conflicts between the java data type (Short, Integer, Long, Float, Double, String, etc) and HBase bytearray order, which means as long as the format saved in HBase is consistent with the Java native data format, the libary will be able to take care of pruning and comparison instead of relying on a specific import tools.

## Data locality
When the spark work node co-located with hbase region servers, data locality is achieved by identifying the region server location, and co-locate the executor with the region server. Each executor will only perform Scan/BulkGet on the part of the data that co-locates on the same host. 

## Predicate pushdown
The lib use existing standard HBase filter provided by HBase and does not operate on the coprocessor. 

## Partition Pruning
By extracting the row key from the predicates, we split the scan/BulkGet into multiple non-overlapping regions, only the region servers that have the requested data will perform scan/BulkGet. Currently, the partition pruning is performed on the first dimension of the row keys. Note that the WHERE conditions need to be defined carefully. Otherwise, the result scanning may includes a region larger than user expectd. For example, following condition will result in a full scan (rowkey1 is the first dimension of the rowkey, and column is a regular hbase column).
         WHERE rowkey1 > "abc" OR column = "xyz"

## Scanning and BulkGet
Both are exposed to users by specifying WHERE CLAUSE, e.g., where column > x and column < y for scan and where column = x for get. All the operations are performed in the executors, and driver only constructs these operations. Internally we will convert them to scan or get or combination of both, which return Iterator[Row] to catalyst engine. 

## 
Creatable DataSource  The libary support both read/write from/to HBase.

###Compile

    mvn package -DskipTests

###Running Tests and Examples
Run test

    mvn clean package test

Run indiviudal test

    mvn -DwildcardSuites=org.apache.spark.sql.DefaultSourceSuite test
Run SHC examples

    ./bin/spark-submit --verbose --class org.apache.spark.sql.execution.datasources.hbase.examples.HBaseSource --master yarn-cluster --packages com.hortonworks:shc:1.0.0-1.6-s_2.10 --repositories http://repo.hortonworks.com/content/groups/public/ --files /usr/hdp/current/hbase-client/conf/hbase-site.xml shc-examples-1.1.0-1.6-s_2.10-SNAPSHOT.jar

The following illustrates how to run your application in real hbase cluster. You need to provide the hbase-site.xml. It may subject to change based on your specific cluster configuration.

    ./bin/spark-submit  --class your.application.class --master yarn-client     --num-executors 2     --driver-memory 512m     --executor-memory 512m     --executor-cores 1 --packages com.hortonworks:shc:1.0.0-1.6-s_2.10 --repositories http://repo.hortonworks.com/content/groups/public/  --files /etc/hbase/conf/hbase-site.xml /To/your/application/jar

**Note**: Running Spark applications with this connector, HBase jars of version 1.1.2 will be pulled by default. If Phoenix is enabled on HBase cluster, you need to use "--jars" to pass "phoenix-server.jar". For example:

    ./bin/spark-submit  --class your.application.class --master yarn-client     --num-executors 2     --driver-memory 512m     --executor-memory 512m     --executor-cores 1 --packages com.hortonworks:shc:1.0.0-1.6-s_2.10 --repositories http://repo.hortonworks.com/content/groups/public/ --jars /usr/hdp/current/phoenix-client/phoenix-server.jar --files /etc/hbase/conf/hbase-site.xml /To/your/application/jar

##Application Usage
The following illustrates the basic procedure on how to use the connector. For more details and advanced use case, such as Avro and composite key support, please refer to the examples in the repository.

### Defined the HBase catalog

    def catalog = s"""{
            |"table":{"namespace":"default", "name":"table1"},
            |"rowkey":"key",
            |"columns":{
              |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
              |"col1":{"cf":"cf1", "col":"col1", "type":"boolean"},
              |"col2":{"cf":"cf2", "col":"col2", "type":"double"},
              |"col3":{"cf":"cf3", "col":"col3", "type":"float"},
              |"col4":{"cf":"cf4", "col":"col4", "type":"int"},
              |"col5":{"cf":"cf5", "col":"col5", "type":"bigint"},
              |"col6":{"cf":"cf6", "col":"col6", "type":"smallint"},
              |"col7":{"cf":"cf7", "col":"col7", "type":"string"},
              |"col8":{"cf":"cf8", "col":"col8", "type":"tinyint"}
            |}
          |}""".stripMargin
         
The above defines a schema for a HBase table with name as table1, row key as key and a number of columns (col1-col8). Note that the rowkey also has to be defined in details as a column (col0), which has a specific cf (rowkey).

### Write to HBase table to populate data

    sc.parallelize(data).toDF.write.options(
      Map(HBaseTableCatalog.tableCatalog -> catalog, HBaseTableCatalog.newTable -> "5"))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .save()
      
Given a data frame with specified schema, above will create an HBase table with 5 regions and save the data frame inside. Note that if HBaseTableCatalog.newTable is not specified, the table has to be pre-created.

### Perform data frame operation on top of HBase table

    def withCatalog(cat: String): DataFrame = {
      sqlContext
      .read
      .options(Map(HBaseTableCatalog.tableCatalog->cat))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .load()
    }
  
### Complicated query

    val df = withCatalog(catalog)
    val s = df.filter((($"col0" <= "row050" && $"col0" > "row040") ||
      $"col0" === "row005" ||
      $"col0" === "row020" ||
      $"col0" ===  "r20" ||
      $"col0" <= "row005") &&
      ($"col4" === 1 ||
      $"col4" === 42))
      .select("col0", "col1", "col4")
    s.show
    
### SQL support

    // Load the dataframe
    val df = withCatalog(catalog)
    //SQL example
    df.registerTempTable("table")
    sqlContext.sql("select count(col1) from table").show

## Configuring Spark-package
Users can use the Spark-on-HBase connector as a standard Spark package. To include the package in your Spark application use:

_**Note**: com.hortonworks:shc:1.0.0-1.6-s_2.10 has not been uploaded to [spark-packages.org](https://spark-packages.org/package/hortonworks-spark/shc), but will be there soon._

spark-shell, pyspark, or spark-submit

    $SPARK_HOME/bin/spark-shell --packages com.hortonworks:shc:1.0.0-1.6-s_2.10

Users can include the package as the dependency in your SBT file as well. The format is the spark-package-name:version

    spDependencies += “com.hortonworks/shc:1.0.0-1.6-s_2.10”

## Running in secure cluster

For running in a Kerberos enabled cluster, the user has to include HBase related jars into the classpath as the HBase token
retrieval and renewal is done by Spark, and is independent of the connector. In other words, the user needs to initiate the
environment in the normal way, either through kinit or by providing principal/keytab.  The following examples show how to run
in a secure cluster with both yarn-client and yarn-cluster mode. Note that SPARK_CLASSPATH has to be set for both modes, and
the example jar is just a placeholder for Spark.

    export SPARK_CLASSPATH=/usr/hdp/current/hbase-client/lib/hbase-common.jar:/usr/hdp/current/hbase-client/lib/hbase-client.jar:/usr/hdp/current/hbase-client/lib/hbase-server.jar:/usr/hdp/current/hbase-client/lib/hbase-protocol.jar:/usr/hdp/current/hbase-client/lib/guava-12.0.1.jar

Suppose hrt_qa is a headless account, user can use following command for kinit:

    kinit -k -t /tmp/hrt_qa.headless.keytab hrt_qa

    /usr/hdp/current/spark-client/bin/spark-submit --class your.application.class --master yarn-client --packages com.hortonworks:shc:1.0.0-1.6-s_2.10 --repositories http://repo.hortonworks.com/content/groups/public/ --num-executors 4 --driver-memory 512m --executor-memory 512m --executor-cores 1 /To/your/application/jar

    /usr/hdp/current/spark-client/bin/spark-submit --class your.application.class --master yarn-cluster --files /etc/hbase/conf/hbase-site.xml --packages com.hortonworks:shc:1.0.0-1.6-s_2.10 --repositories http://repo.hortonworks.com/content/groups/public/ --num-executors 4 --driver-memory 512m --executor-memory 512m --executor-cores 1 /To/your/application/jar

#### TODO:

    val complex = s"""MAP<int, struct<varchar:string>>"""
    val schema =
      s"""{"namespace": "example.avro",
         |   "type": "record",      "name": "User",
         |    "fields": [      {"name": "name", "type": "string"},
         |      {"name": "favorite_number",  "type": ["int", "null"]},
         |        {"name": "favorite_color", "type": ["string", "null"]}      ]    }""".stripMargin
    val catalog = s"""{
            |"table":{"namespace":"default", "name":"htable"},
            |"rowkey":"key1:key2",
            |"columns":{
              |"col1":{"cf":"rowkey", "col":"key1", "type":"binary"},
              |"col2":{"cf":"rowkey", "col":"key2", "type":"double"},
              |"col3":{"cf":"cf1", "col":"col1", "avro":"schema1"},
              |"col4":{"cf":"cf1", "col":"col2", "type":"string"},
              |"col5":{"cf":"cf1", "col":"col3", "type":"double",        "sedes":"org.apache.spark.sql.execution.datasources.hbase.DoubleSedes"},
              |"col6":{"cf":"cf1", "col":"col4", "type":"$complex"}
            |}
          |}""".stripMargin
       
    val df = sqlContext.read.options(Map("schema1"->schema, HBaseTableCatalog.tableCatalog->catalog)).format("org.apache.spark.sql.execution.datasources.hbase").load()
    df.write.options(Map("schema1"->schema, HBaseTableCatalog.tableCatalog->catalog)).format("org.apache.spark.sql.execution.datasources.hbase").save()
          

Above illustrates our next step, which includes composite key support, complex data types, support of customerized sedes and avro. Note that although all the major pieces are included in the current code base, but it may not be functioning now.


## Trademarks

Apache®, Apache Spark, Apache HBase, Spark, and HBase are either registered trademarks or trademarks of the Apache Software Foundation in the United States and/or other countries.

