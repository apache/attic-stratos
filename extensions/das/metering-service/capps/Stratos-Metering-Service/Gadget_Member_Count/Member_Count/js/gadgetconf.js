var gadgetConfig = {
    "id": "Member_Count",
    "title": "Member_Count",
    "datasource": "MEMBER_COUNT",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "StartTime",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "EndTime",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "ApplicationId",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "ClusterId",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "CreatedInstanceCount",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "InitializedInstanceCount",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "ActiveInstanceCount",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "TerminatedInstanceCount",
            "DATA_TYPE": "int"
        }
    ],
    "maxUpdateValue": 10,
    "chartConfig": {
        "chartType": "line",
        "yAxis": [4, 5, 6, 7],
        "xAxis": 1,
        "interpolationMode": "line"
    },
    "domain": "carbon.super"
};