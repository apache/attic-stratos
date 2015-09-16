var gadgetConfig = {
        "id": "Member_Status",
        "title": "Member Status",
        "datasource": "MEMBER_STATUS",
        "type": "batch",
        "columns": [
            {
                "COLUMN_NAME": "Time",
                "DATA_TYPE": "varchar"
            },
            {
                "COLUMN_NAME": "MemberId",
                "DATA_TYPE": "varchar"
            },
            {
                "COLUMN_NAME": "MemberStatus",
                "DATA_TYPE": "varchar"
            }
        ],
        "maxUpdateValue": 10,
        "chartConfig": {
            "chartType": "tabular",
            "xAxis": 1
        }
        ,
        "domain": "carbon.super"
    }
    ;