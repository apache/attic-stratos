var gadgetConfig = {
    "id": "Member_Information",
    "title": "Member Information",
    "datasource": "MEMBER_INFORMATION",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "MemberId",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "InstanceType",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "ImageId",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "HostName",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "PrivateIPAddresses",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "PublicIPAddresses",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "Hypervisor",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "CPU",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "RAM",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "OSName",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "OSVersion",
            "DATA_TYPE": "varchar"
        }
    ],
    "maxUpdateValue": 10,
    "chartConfig": {
        "chartType": "tabular", "xAxis": 0
    }
    ,
    "domain": "carbon.super"
};