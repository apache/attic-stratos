/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
var jsonDataLoadAvg;
var jsonDataFlightRequest;
var jsonDataMemoryAvg;
var type;

function DurationOnClick(element){
    var buttonId = element.id;
    var buttonName = element.name;
    var buttonIds = ["1Hour", "30Min", "1Day" , "1Week", "1Month"];
    var chartType = $('#chartType').val();
    var idValue = $('#IdValue').val();

    if ('InFlight' != buttonName) {
        if ('LoadAverage' == buttonName) {
            $('#chart2').empty();
            $('#chart2').html('<i class="fa fa-spinner fa-pulse fa-4x"></i>');

        } else {
            $('#chart3').empty();
            $('#chart3').html('<i class="fa fa-spinner fa-pulse fa-4x"></i>');

        }
    } else {
        $('#chart1').empty();
        $('#chart1').html('<i class="fa fa-spinner fa-pulse fa-4x"></i>');

    }

    $(buttonIds).each(function(i, e) {

        $('#'+e+'[name='+buttonName+']').css("color", "");
    });

    $('#'+buttonId+'[name='+buttonName+']').css("color", "Blue");

    $.ajax({
        type: "GET",
        url: caramel.context + "/controllers/healthStatistics/healthStatistics_getrequest.jag",
        dataType: 'json',
        data: {"formtype": buttonName, "idValue":idValue, "chartType": chartType, "duration": buttonId},
        success: function (data) {

            if('error' != data.status){
                if('InFlight' == buttonName){
                    $('#chart1').empty();
                    jsonDataFlightRequest = data;
                    inFlightRequestCountChart();
                }
                else if('LoadAverage' == buttonName){
                    $('#chart2').empty();
                    jsonDataLoadAvg = data;
                    loadAverageChart();
                }
                else{
                    $('#chart3').empty();
                    jsonDataMemoryAvg = data;
                    memoryAverageDataChart();
                }

            }else{

                var n = noty({text: data.message, layout: 'bottomRight', type: 'error'});
            }


        }

    }).always(function () {});
}

function bodyOnLoad (loadAverageData,flightRequestDetails,memoryAverageDetails) {


    jsonDataLoadAvg = loadAverageData;

    jsonDataFlightRequest = flightRequestDetails;

    jsonDataMemoryAvg = memoryAverageDetails;

    loadAverageChart();
    memoryAverageDataChart();
    inFlightRequestCountChart();

}

function loadAverageChart () {
    var margin = {top: 20, right: 20, bottom: 30, left: 50},
        width = 600 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom;

    var parseDate = d3.time.format("%d-%b-%y").parse;

    var x = d3.time.scale()
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);

    var xAxis = d3.svg.axis()
        .scale(x)
        .orient("bottom");

    var yAxis = d3.svg.axis()
        .scale(y)
        .orient("left");

    var area = d3.svg.area()
        .x(function(d) { return x(d.timeStamp); })
        .y0(height)
        .y1(function(d) {return y(d.memberAverageLoadAverage); });

    var tip = d3.tip()
        .attr('class', 'd3-tip')
        .offset([-10, 0])
        .html(function(d) {
            return "<strong>Load Average:</strong> <span style='color:red'>" + d.memberAverageLoadAverage +
                "</span> <strong>Time:</strong> <span style='color:red'>" + new Date(d.timeStamp) + "</span>";
        })

    var svg = d3.select("#chart2").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    svg.call(tip);

    var data = jsonDataLoadAvg;

    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([0, d3.max(data, function(d) { return d.memberAverageLoadAverage; })]);

    svg.append("path")
        .datum(data)
        .attr("class", "area1")
        .attr("d", area);

    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    svg.append("g")
        .attr("class", "y axis")
        .call(yAxis)
        .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", 6)
        .attr("dy", ".71em")
        .style("text-anchor", "end")
        .text("Load Average");

    svg.selectAll(".area")
        .data(data)
        .enter().append("rect")
        .attr("class", "area1")
        .attr("x", function(d) { return x(d.timeStamp); })
        .attr("width", 1)
        .attr("y", function(d) { return y(d.memberAverageLoadAverage); })
        .attr("height", function(d) { return height - y(d.memberAverageLoadAverage); })
        .on('mouseover', tip.show)
        .on('mouseout', tip.hide)
}

function inFlightRequestCountChart () {

    var margin = {top: 10, right: 10, bottom: 100, left: 40},
        margin2 = {top: 220, right: 10, bottom: 20, left: 40},
        width = 600 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom,
        height2 = 300 - margin2.top - margin2.bottom;

    var parseDate = d3.time.format("%b %Y").parse;

    var x = d3.time.scale().range([0, width]),
        x2 = d3.time.scale().range([0, width]),
        y = d3.scale.linear().range([height, 0]),
        y2 = d3.scale.linear().range([height2, 0]);

    var xAxis = d3.svg.axis().scale(x).orient("bottom"),
        xAxis2 = d3.svg.axis().scale(x2).orient("bottom"),
        yAxis = d3.svg.axis().scale(y).orient("left");

    var brush = d3.svg.brush()
        .x(x2)
        .on("brush", brushed);

    var line = d3.svg.line()
        .interpolate("linear")
        .x(function(d) { return x(d.timeStamp); })
        .y(function(d) { return y(d.inFlightRequestCount); });

    var tip = d3.tip()
        .attr('class', 'd3-tip')
        .offset([-10, 0])
        .html(function(d) {
            return "<strong>Flight Request Count:</strong> <span style='color:red'>" + d.inFlightRequestCount +
                "</span> <strong>Time:</strong> <span style='color:red'>" + new Date(d.timeStamp) + "</span>";
        })

    var line2 = d3.svg.line()
        .interpolate("linear")
        .x(function(d) { return x2(d.timeStamp); })
        .y(function(d) { return y2(d.inFlightRequestCount); });

    var svg = d3.select("#chart1").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom);

    svg.append("defs").append("clipPath")
        .attr("id", "clip")
        .append("rect")
        .attr("width", width)
        .attr("height", height);

    var focus = svg.append("g")
        .attr("class", "focus")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var context = svg.append("g")
        .attr("class", "context")
        .attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");


    svg.call(tip);

    var data = jsonDataFlightRequest;

    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([0, d3.max(data, function(d) { return d.inFlightRequestCount; })]);


    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([0, d3.max(data, function(d) { return d.inFlightRequestCount; })]);
    x2.domain(x.domain());
    y2.domain(y.domain());

    focus.append("path")
        .datum(data)
        .attr("class", "line")
        .attr("d", line);

    focus.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    focus.append("g")
        .attr("class", "y axis")
        .call(yAxis);

    context.append("path")
        .datum(data)
        .attr("class", "line")
        .attr("d", line2);

    context.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height2 + ")")
        .call(xAxis2);

    context.append("g")
        .attr("class", "x brush")
        .call(brush)
        .selectAll("rect")
        .attr("y", -6)
        .attr("height", height2 + 7);

    svg.selectAll(".line2")
        .data(data)
        .enter().append("rect")
        .attr("class", "line")
        .attr("x", function(d) { return x(d.timeStamp); })
        .attr("width", 1)
        .attr("y", function(d) { return y(d.inFlightRequestCount); })
        .attr("height", function(d) { return height - y(d.inFlightRequestCount); })
        .on('mouseover', tip.show)
        .on('mouseout', tip.hide)


    function brushed() {
        x.domain(brush.empty() ? x2.domain() : brush.extent());
        focus.select(".line").attr("d", line);
        focus.select(".x.axis").call(xAxis);
    }

    function type(d) {
        d.timeStamp = parseDate(d.timeStamp);
        d.inFlightRequestCount = +d.inFlightRequestCount;
        return d;
    }
}

function memoryAverageDataChart () {
    var margin = {top: 10, right: 10, bottom: 100, left: 40},
        margin2 = {top: 220, right: 10, bottom: 20, left: 40},
        width = 600 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom,
        height2 = 300 - margin2.top - margin2.bottom;

    var parseDate = d3.time.format("%b %Y").parse;

    var x = d3.time.scale().range([0, width]),
        x2 = d3.time.scale().range([0, width]),
        y = d3.scale.linear().range([height, 0]),
        y2 = d3.scale.linear().range([height2, 0]);

    var xAxis = d3.svg.axis().scale(x).orient("bottom"),
        xAxis2 = d3.svg.axis().scale(x2).orient("bottom"),
        yAxis = d3.svg.axis().scale(y).orient("left");

    var brush = d3.svg.brush()
        .x(x2)
        .on("brush", brushed);

    var area = d3.svg.area()
        .interpolate("linear")
        .x(function(d) { return x(d.timeStamp); })
        .y0(height)
        .y1(function(d) { return y(d.memberAverageMemoryConsumption); });

    var area2 = d3.svg.area()
        .interpolate("linear")
        .x(function(d) { return x2(d.timeStamp); })
        .y0(height2)
        .y1(function(d) { return y2(d.memberAverageMemoryConsumption); });

    var svg = d3.select("#chart3").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom);

    svg.append("defs").append("clipPath")
        .attr("id", "clip")
        .append("rect")
        .attr("width", width)
        .attr("height", height);

    var focus = svg.append("g")
        .attr("class", "focus")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var context = svg.append("g")
        .attr("class", "context")
        .attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");

    var data = jsonDataMemoryAvg;

    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([-2, d3.max(data, function(d) { return d.memberAverageMemoryConsumption; })]);


    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([-2, d3.max(data, function(d) { return d.memberAverageMemoryConsumption; })]);
    x2.domain(x.domain());
    y2.domain(y.domain());

    focus.append("path")
        .datum(data)
        .attr("class", "area")
        .attr("d", area);

    focus.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    focus.append("g")
        .attr("class", "y axis")
        .call(yAxis);

    context.append("path")
        .datum(data)
        .attr("class", "area")
        .attr("d", area2);

    context.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height2 + ")")
        .call(xAxis2);

    context.append("g")
        .attr("class", "x brush")
        .call(brush)
        .selectAll("rect")
        .attr("y", -6)
        .attr("height", height2 + 7);


    function brushed() {
        x.domain(brush.empty() ? x2.domain() : brush.extent());
        focus.select(".area").attr("d", area);
        focus.select(".x.axis").call(xAxis);
    }

    function type(d) {
        d.timeStamp = parseDate(d.timeStamp);
        d.memberAverageMemoryConsumption = +d.memberAverageMemoryConsumption;
        return d;
    }
}