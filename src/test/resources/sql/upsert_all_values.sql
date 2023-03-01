upsert into ${tableName}(key,
                         c_Bool,
                         c_Int32,
                         c_Int64,
                         c_Uint8,
                         c_Uint32,
                         c_Uint64,
                         c_Float,
                         c_Double,
                         c_Bytes,
                         c_Text,
                         c_Json,
                         c_JsonDocument,
                         c_Yson,
                         c_Date,
                         c_Datetime,
                         c_Timestamp,
                         c_Interval,
                         c_Decimal)
values
    (1,
    true,
    cast(2000000001 as Int32),
    cast(2000000000001 as Int64),
    cast(100 as Uint8),
    cast(2000000002 as Uint32),
    cast(2000000000002 as Uint64), -- TODO: check very large uint64? How it should fit to JVM long value?
    cast(123456.78 as Float),
    cast(123456789.123456789 as Double),
    cast ('bytes array' as Bytes),
    cast ('text text text' as Text),
    cast ('{"key": "value Json"}' as Json),
    cast ('{"key": "value JsonDocument"}' as JsonDocument),
    Yson(@@{key="value yson"}@@),
    cast (3111 as Date),
    cast (3111111 as DateTime),
    cast (3111112 as Timestamp),
    cast (3111113 as Interval),
    cast('3.335' as Decimal(22, 9))
    ),
    (2,
    false,
    cast(-2000000001 as Int32),
    cast(-2000000000001 as Int64),
    cast(200 as Uint8),
    cast(4000000002 as Uint32),
    cast(4000000000002 as Uint64),
    cast(-123456.78 as Float),
    cast(-123456789.123456789 as Double),
    cast ('' as Bytes),
    cast ('' as Text),
    cast ('' as Json),
    cast ('' as JsonDocument),
    Yson(@@""@@),
    cast (3112 as Date),
    cast (3112111 as DateTime),
    cast (3112112 as Timestamp),
    cast (3112113 as Interval),
    cast('-3.335' as Decimal(22, 9))
    ),
    (3,
    false,
    cast(0 as Int32),
    cast(0 as Int64),
    cast(0 as Uint8),
    cast(0 as Uint32),
    cast(0 as Uint64),
    cast(0 as Float),
    cast(0 as Double),
    cast ('0' as Bytes),
    cast ('0' as Text),
    cast ('' as Json),
    cast ('' as JsonDocument),
    Yson(@@0@@),
    cast (0 as Date),
    cast (0 as DateTime),
    cast (0 as Timestamp),
    cast (0 as Interval),
    cast('0' as Decimal(22, 9))
    ),
    (4,
    true,
    cast(1 as Int32),
    cast(1 as Int64),
    cast(1 as Uint8),
    cast(1 as Uint32),
    cast(1 as Uint64),
    cast(1 as Float),
    cast(1 as Double),
    cast ('file:///tmp/report.txt' as Bytes),
    cast ('https://ydb.tech' as Text),
    cast ('{}' as Json),
    cast ('{}' as JsonDocument),
    Yson(@@1@@),
    cast (1 as Date),
    cast (1 as DateTime),
    cast (1 as Timestamp),
    cast (1 as Interval),
    cast('1' as Decimal(22, 9))
    ),
    (5,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null
    )
