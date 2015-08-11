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

function DurationOnClick(ref) {

    var buttonName = $(ref).attr("name");

    $("button[name=" + buttonName + "]").each(function () {
        $(this).css("color", "");
    });

    $(ref).css("color", "Blue");
}

//onclick function which will trigger the option for 30mints,1 hour etc
function DurationOnClick(element){

    var buttonId = element.id;
    var buttonName = $(element).attr("name");
    var chartType = $('#chartType').val();
    var idValue = $('#IdValue').val();

    //here the spinner will indicate untill the data loads to the map
    if ('InFlight' != buttonName) {

        if ('LoadAverage' == buttonName) {
            $('#container2').empty();
            $('#container2').html('<i class="fa fa-spinner fa-pulse fa-4x"></i>');

        } else {

            $('#container1').empty();
            $('#container1').html('<i class="fa fa-spinner fa-pulse fa-4x"></i>');

        }
    } else {
        $('#container3').empty();
        $('#container3').html('<i class="fa fa-spinner fa-pulse fa-4x"></i>');


    }

    //after user click the option here we are changing the option black to red.
    $("button[name=" + buttonName + "]").each(function () {
        $(this).css("color", "");
    });

    $(element).css("color", "Red");
    restCaller(buttonName,idValue,chartType,buttonId);

}

//ajax call to the UI back end get the data to the charts 
function restCaller(buttonName,idValue,chartType,buttonId){

    $.ajax({
        type: "GET",
        url: caramel.context + "/controllers/healthStatistics/healthStatistics_getrequest.jag",
        dataType: 'json',
        data: {"formtype": buttonName, "idValue":idValue, "chartType": chartType, "duration": buttonId},
        success: function (data) {

            if('error' != data.status){

                if('InFlight' == buttonName){

                    $('#lineChartSVGchart3').empty();
                    $('#container3').empty();
                    $('#container3').append(html3);
                    jsonDataFlightRequest = data;
                    inFlightRequestCountChart();


                }
                else if('LoadAverage' == buttonName){

                    $('#lineChartSVGchart2').empty();
                    $('#container2').empty();
                    $('#container2').append(html2);
                    jsonDataLoadAvg = data;
                    loadAverageChart();

                }
                else{

                    $('#lineChartSVGchart1').empty();
                    $('#container1').empty();
                    $('#container1').append(html1);
                    jsonDataMemoryAvg = data;
                    memoryAverageDataChart();

                }

            }else{

                //print error message in any case.
                var n = noty({text: data.message, layout: 'bottomRight', type: 'error'});
            }


        }

    }).always(function () {});


}

//body onload function will trigger when the page loads
function bodyOnLoad () {

    var buttonNames;
    var chartType = $('#chartType').val();
    if(chartType!="Cluster"){

        buttonNames = ["LoadAverage", "MemoryConsumption"];

    }else{

        buttonNames = ["InFlight", "LoadAverage", "MemoryConsumption"];
    }

    buttonNames.forEach(function(entry) {
        initialLoad(entry,chartType);

    });

}

//initialy acalling to the REST API and get data for the chart
function initialLoad(buttonName,chartType){

    var idValue = $('#IdValue').val();
    var buttonId = "1Hour";
    restCaller(buttonName,idValue,chartType,buttonId);

}


//this is the load avergae chart for cluster/member
function loadAverageChart () {

    var margin = {top: 10, right: 10, bottom: 100, left: 40},
        margin2 = {top: 220, right: 10, bottom: 20, left: 40},
        width = 600 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom,
        height2 = 300 - margin2.top - margin2.bottom;

    var parseDate = d3.time.format("%b %Y").parse;

    var x = d3.time.scale().range([0, width]),
        x2 = d3.time.scale().range([0, width]),

    //defines the maximum and minimum values we have to plot in the available space.
        y = d3.scale.linear().range([height, 0]),
        y2 = d3.scale.linear().range([height2, 0]);

    //provides an API method called d3.svg.axis to create axes.
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
        .y1(function(d) { return y(d.memberAverageLoadAverage); });

    var area2 = d3.svg.area()
        .interpolate("linear")
        .x(function(d) { return x2(d.timeStamp); })
        .y0(height2)
        .y1(function(d) { return y2(d.memberAverageLoadAverage); });

    var svg = d3.select("#chart2").append("svg")
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

    var data = jsonDataLoadAvg;

    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([0, d3.max(data, function(d) { return d.memberAverageLoadAverage; })]);


    x.domain(d3.extent(data, function(d) { return d.timeStamp; }));
    y.domain([0, d3.max(data, function(d) { return d.memberAverageLoadAverage; })]);
    x2.domain(x.domain());
    y2.domain(y.domain());

    focus.append("path")
        .datum(data)
        .attr("class", "area")
        .attr("d", area);

    //append the created X axis to the svg container
    focus.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    //append the created Y axis to the svg container
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
        d.memberAverageLoadAverage = +d.memberAverageLoadAverage;
        return d;
    }




}

function inFlightRequestCountChart () {



    var DURATION = 50;
    var DELAY    = 500;

    function drawLineChart( elementId, data ) {
        // parse helper functions on top
        //var parse = d3.time.format( '%Y-%m-%d' ).parse;
        // data manipulation first

        //sample2.forEach(function(d) { d.time = new Date(d.time * 1000); });


        data.forEach(function(data) { data.timeStamp = new Date(data.timeStamp); });
        // data manipulation first


        // TODO code duplication check how you can avoid that
        var containerEl = document.getElementById( elementId ),
            width       = containerEl.clientWidth,
            height      = width * 0.4,
            margin      = {
                top    : 30,
                right  : 10,
                left   : 10
            },

            detailWidth  = 98,
            detailHeight = 55,
            detailMargin = 10,

            container   = d3.select( containerEl ),
            svg         = container.select( 'svg' )
                .attr( 'width', width )
                .attr( 'height', height + margin.top ),

            x          = d3.time.scale().range( [ 0, width - detailWidth ] ),
            xAxis      = d3.svg.axis().scale( x )
                .ticks ( 8 )
                .tickSize( -height ),
            xAxisTicks = d3.svg.axis().scale( x )
                .ticks( 16 )
                .tickSize( -height )
                .tickFormat( '' ),
            y          = d3.scale.linear().range( [ height, 0 ] ),
            yAxisTicks = d3.svg.axis().scale( y )
                .ticks( 12 )
                .tickSize( width )
                .tickFormat( '' )
                .orient( 'right' ),

            area = d3.svg.area()
                .interpolate( 'linear' )
                .x( function( d )  { return x( d.timeStamp ) + detailWidth / 2; } )
                .y0( height )
                .y1( function( d ) { return y( d.inFlightRequestCount ); } ),

            line = d3.svg.line()
                .interpolate( 'linear' )
                .x( function( d ) { return x( d.timeStamp ) + detailWidth / 2; } )
                .y( function( d ) { return y( d.inFlightRequestCount ); } ),

            startData = data.map( function( datum ) {
                return {
                    timeStamp  : datum.timeStamp,
                    value : 0
                };
            } ),

            circleContainer;

        // Compute the minimum and maximum date, and the maximum price.
        x.domain( [ data[ 0 ].timeStamp, data[ data.length - 1 ].timeStamp ] );
        // hacky hacky hacky :(
        y.domain( [ 0, d3.max( data, function( d ) { return d.inFlightRequestCount; } ) + 100 ] );

        svg.append( 'g' )
            .attr( 'class', 'lineChart--xAxisTicks' )
            .attr( 'transform', 'translate(' + detailWidth / 2 + ',' + height + ')' )
            .call( xAxisTicks );

        svg.append( 'g' )
            .attr( 'class', 'lineChart--xAxis' )
            .attr( 'transform', 'translate(' + detailWidth / 2 + ',' + ( height + 7 ) + ')' )
            .call( xAxis );

        svg.append( 'g' )
            .attr( 'class', 'lineChart--yAxisTicks' )
            .call( yAxisTicks );

        // Add the line path.
        svg.append( 'path' )
            .datum( startData )
            .attr( 'class', 'lineChart--areaLine' )
            .attr( 'd', line )
            .transition()
            .duration( DURATION )
            .delay( DURATION / 2 )
            .attrTween( 'd', tween( data, line ) )
            .each( 'end', function() {
                drawCircles( data );
            } );


        // Add the area path.
        svg.append( 'path' )
            .datum( startData )
            .attr( 'class', 'lineChart--area' )
            .attr( 'd', area )
            .transition()
            .duration( DURATION )
            .attrTween( 'd', tween( data, area ) );

        // Helper functions!!!
        function drawCircle( datum, index ) {
            circleContainer.datum( datum )
                .append( 'circle' )
                .attr( 'class', 'lineChart--circle' )
                .attr( 'r', 0 )
                .attr(
                'cx',
                function( d ) {
                    return x( d.timeStamp ) + detailWidth / 2;
                }
            )
                .attr(
                'cy',
                function( d ) {
                    return y( d.inFlightRequestCount );
                }
            )
                .on( 'mouseenter', function( d ) {
                    d3.select( this )
                        .attr(
                        'class',
                        'lineChart--circle lineChart--circle__highlighted'
                    )
                        .attr( 'r', 4 );

                    d.active = true;

                    showCircleDetail( d );
                } )
                .on( 'mouseout', function( d ) {
                    d3.select( this )
                        .attr(
                        'class',
                        'lineChart--circle'
                    )
                        .attr( 'r', 4 );

                    if ( d.active ) {
                        hideCircleDetails();

                        d.active = false;
                    }
                } )
                .on( 'click touch', function( d ) {
                    if ( d.active ) {
                        showCircleDetail( d )
                    } else {
                        hideCircleDetails();
                    }
                } )
                .transition()
                .delay( DURATION / 10 * index )
                .attr( 'r', 4 );
        }

        function drawCircles( data ) {
            circleContainer = svg.append( 'g' );

            data.forEach( function( datum, index ) {
                drawCircle( datum, index );
            } );
        }

        function hideCircleDetails() {
            circleContainer.selectAll( '.lineChart--bubble' )
                .remove();
        }

        function showCircleDetail( data ) {
            var details = circleContainer.append( 'g' )
                .attr( 'class', 'lineChart--bubble' )
                .attr(
                'transform',
                function() {
                    var result = 'translate(';

                    result += x( data.timeStamp );
                    result += ', ';
                    result += y( data.inFlightRequestCount ) - detailHeight - detailMargin;
                    result += ')';

                    return result;
                }
            );

            details.append( 'path' )
                .attr( 'd', 'M2.99990186,0 C1.34310181,0 0,1.34216977 0,2.99898218 L0,47.6680579 C0,49.32435 1.34136094,50.6670401 3.00074875,50.6670401 L44.4095996,50.6670401 C48.9775098,54.3898926 44.4672607,50.6057129 49,54.46875 C53.4190918,50.6962891 49.0050244,54.4362793 53.501875,50.6670401 L94.9943116,50.6670401 C96.6543075,50.6670401 98,49.3248703 98,47.6680579 L98,2.99898218 C98,1.34269006 96.651936,0 95.0000981,0 L2.99990186,0 Z M2.99990186,0' )
                .attr( 'width', detailWidth )
                .attr( 'height', detailHeight );

            var text = details.append( 'text' )
                .attr( 'class', 'lineChart--bubble--text' );


            text.append( 'tspan' )
                .attr( 'class', 'lineChart--bubble--value' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 4 * 3 )
                .attr( 'text-anchor', 'middle' )
                .text( data.inFlightRequestCount );
            console.info(data.inFlightRequestCount);
        }

        function tween( b, callback ) {
            return function( a ) {
                var i = d3.interpolateArray( a, b );

                return function( t ) {
                    return callback( i ( t ) );
                };
            };
        }
    }

    var data = jsonDataFlightRequest;
    drawLineChart('chart3',data);


function memoryAverageDataChart () {

    var margin = {top: 10, right: 10, bottom: 100, left: 40},
        margin2 = {top: 220, right: 10, bottom: 20, left: 40},
        width = 600 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom,
        height2 = 300 - margin2.top - margin2.bottom;

    var parseDate = d3.time.format("%b %Y").parse;

    var x = d3.time.scale().range([0, width]),
        x2 = d3.time.scale().range([0, width]),

    //defines the maximum and minimum values we have to plot in the available space.
        y = d3.scale.linear().range([height, 0]),
        y2 = d3.scale.linear().range([height2, 0]);

    //provides an API method called d3.svg.axis to create axes.
    var xAxis = d3.svg.axis().scale(x).orient("bottom"),
        xAxis2 = d3.svg.axis().scale(x2).orient("bottom"),
        yAxis = d3.svg.axis().scale(y).orient("left");

    var brush = d3.svg.brush()
        .x(x2)
        .on("brush", brushed);

    var area = d3.svg.area()
        .interpolate("linear")
        .x(function (d) {
            return x(d.timeStamp);
        })
        .y0(height)
        .y1(function (d) {
            return y(d.memberAverageMemoryConsumption);
        });

    var area2 = d3.svg.area()
        .interpolate("linear")
        .x(function (d) {
            return x2(d.timeStamp);
        })
        .y0(height2)
        .y1(function (d) {
            return y2(d.memberAverageMemoryConsumption);
        });

    var tip = d3.tip()
        .attr('class', 'd3-tip')
        .offset([-10, 0])
        .html(function (d) {
            return "<strong>Flight Request Count:</strong> <span style='color:red'>" + d.memberAverageMemoryConsumption +
                "</span> <strong>Time:</strong> <span style='color:red'>" + new Date(d.timeStamp) + "</span>";
        })

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
    var data = jsonDataMemoryAvg;

    x.domain(d3.extent(data, function (d) {
        return d.timeStamp;
    }));
    y.domain([0, d3.max(data, function (d) {
        return d.memberAverageMemoryConsumption;
    })]);


    x.domain(d3.extent(data, function (d) {
        return d.timeStamp;
    }));
    y.domain([0, d3.max(data, function (d) {
        return d.memberAverageMemoryConsumption;
    })]);
    x2.domain(x.domain());
    y2.domain(y.domain());

    focus.append("path")
        .datum(data)
        .attr("class", "area")
        .attr("d", area);

    //append the created X axis to the svg container
    focus.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    //append the created Y axis to the svg container
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

    svg.selectAll(".area2")
        .data(data)
        .enter().append("rect")
        .attr("class", "area")
        .attr("x", function (d) {
            return x(d.timeStamp);
        })
        .attr("width", 1)
        .attr("y", function (d) {
            return y(d.memberAverageMemoryConsumption);
        })
        .attr("height", function (d) {
            return height - y(d.memberAverageMemoryConsumption);
        })
        .on('mouseover', tip.show)
        .on('mouseout', tip.hide)

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

}

