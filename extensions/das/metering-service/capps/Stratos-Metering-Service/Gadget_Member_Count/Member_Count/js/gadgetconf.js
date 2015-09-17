var gadgetConfig = {
    "id": "Member_Count",
    "title": "Member_Count",
    "datasource": "MEMBER_COUNT",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "Time",
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
        "yAxis": [1, 2, 3, 4],
        "xAxis": 0,
        "interpolationMode": "line"
    },
    "domain": "carbon.super"
};