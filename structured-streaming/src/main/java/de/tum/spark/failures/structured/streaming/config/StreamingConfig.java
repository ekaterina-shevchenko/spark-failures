package de.tum.spark.failures.structured.streaming.config;

import lombok.Data;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

@Data
public class StreamingConfig {

    public static StructType schema = DataTypes.createStructType(new StructField[] {
            DataTypes.createStructField("userId", DataTypes.IntegerType, false),
            DataTypes.createStructField("product", DataTypes.StringType, false),
            DataTypes.createStructField("number", DataTypes.IntegerType, false),
            DataTypes.createStructField("timestamp", DataTypes.LongType, false)});
}
