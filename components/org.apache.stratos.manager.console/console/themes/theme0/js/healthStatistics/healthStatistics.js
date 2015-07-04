var dataJson = null;

var requestInFlight = null;
var memoryConsumptions = null;
var loadAvg = null;

var currentMembers = null;

function bodyOnLoad(data){
    dataJson=data;

    $("#rowTable > a").html("");
    $.each(dataJson, function (i, model) {
        $("#rowTable").append($('<a id=' + model.cluster_id + ' class="block" onClick="return onclickFunction(this)">' +
                '<div class="col-md-4  content-menu-block border-right">' +
                '<div class="content-menu-icon">' +
                '<i class="fa fa-cog fa-fw"></i> </div>' +
                '<div class="content-menu-title">' +
                '<h3>' + model.cluster_id + '</h3> </div>' +
                '<div class="content-menu-description"></div></div></a>')
        );
    });
};

function onclickFunction(elements) {
    $('#mainTitle').html(elements.id);
    var id = elements.id;
    $('#rowTable').hide();

    $.each(dataJson, function (i, model) {
        if (model.cluster_id.trim() == elements.id.trim()) {
            currentMembers = model.member_ids;
            return false;
        }
    });

    $('#menu').show();

    setCharts(requestInFlight, memoryConsumptions, loadAvg);
    currentCluster = id;
    $.each(currentMembers[0], function (i, model) {
        $("#sideMenu").append($('<li><a id=' + i + ' onClick="return onclickSideMenu(this)">' +
                '<i class="fa fa-spinner fa-spin block"></i>' +
                '<span class="title"><font face="verdana">' + model + '</font></span>' +
                '<span class="selected"></span></a></li>')
        );
    });
};

function onclickSideMenu(element){
    $.each(currentMembers[0], function (i, model) {
        if (i.trim() == element.id.trim()) {
            $('#mainTitle').html(model);
        }
    });

    var id = element.id;
    setCharts(requestInFlight, memoryConsumptions, loadAvg);
}

function setCharts(requestInFlight, memoryConsumptions, loadAvg){
    // bar charts
    var barChart = c3.generate({
        bindto: '#chart1',
        data: {
            type: 'bar',
            json: [
                { 'indicator': 'X', 'total': 100 },
                { 'indicator': 'Y', 'total': 200 },
                { 'indicator': 'Z', 'total': 300 }
            ],
            keys: {
                x: 'indicator',
                value: ['total']
            }
        },
        axis: {
            x: {
                label: 'X Axis',
                position: 'outer-center',
                type: 'category',
            }
        },
        bar: {
            width: {
                ratio: 0.5
            }
        },
        grid: {
            y: {
                lines: [{value: 150, text: 'label at value 150'}]
            }
        }
    });

    // spline charts
    var splineChart = c3.generate({
        bindto: '#chart2',
        data: {
            columns: [
                ['data1', 30, 200, 100, 400, 150, 250],
                ['data2', 130, 100, 140, 200, 150, 50],
                ['data3', 10, 120, 90, 40, 250, 150]
            ],
            type: 'spline'
        },
        axis: {
            x: {
                position: 'outer-center',
                label: 'X Axis'
            }
        }
    });

    // spline charts
    var splineChart1 = c3.generate({
        bindto: '#chart3',
        data: {
            columns: [
                ['data1', 30, 200, 100, 400, 150, 250],
                ['data2', 130, 100, 140, 200, 150, 50],
            ],
            type: 'area'
        },
        axis: {
            x: {
                position: 'outer-center',
                label: 'X Axis'
            }
        }
    });
}