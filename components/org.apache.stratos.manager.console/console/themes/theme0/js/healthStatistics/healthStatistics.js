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
var DURATION = 50;
var DELAY    = 500;

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

    var html1 = '<svg id="lineChartSVGchart1" class="lineChart--svg"> <defs> <linearGradient id="lineChart--gradientBackgroundArea" x1="0" x2="0" y1="0" y2="1"> <stop class="lineChart--gradientBackgroundArea--top" offset="0%" /> <stop class="lineChart--gradientBackgroundArea--bottom" offset="100%" /> </linearGradient> </defs> </svg>';
    var html2 = '<svg id="lineChartSVGchart2" class="lineChart--svg"> <defs> <linearGradient id="lineChart--gradientBackgroundArea" x1="0" x2="0" y1="0" y2="1"> <stop class="lineChart--gradientBackgroundArea--top" offset="0%" /> <stop class="lineChart--gradientBackgroundArea--bottom" offset="100%" /> </linearGradient> </defs> </svg>';
    var html3 = '<svg id="lineChartSVGchart3" class="lineChart--svg"> <defs> <linearGradient id="lineChart--gradientBackgroundArea" x1="0" x2="0" y1="0" y2="1"> <stop class="lineChart--gradientBackgroundArea--top" offset="0%" /> <stop class="lineChart--gradientBackgroundArea--bottom" offset="100%" /> </linearGradient> </defs> </svg>';

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
    if("Cluster" != chartType){

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


    function drawLineChart( elementId, data ) {


        // parse helper functions on top
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
                .y1( function( d ) { return y( d.memberAverageLoadAverage ); } ),

            line = d3.svg.line()
                .interpolate( 'linear' )
                .x( function( d ) { return x( d.timeStamp ) + detailWidth / 2; } )
                .y( function( d ) { return y( d.memberAverageLoadAverage ); } ),

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
        y.domain( [ 0, d3.max( data, function( d ) { return d.memberAverageLoadAverage; } ) + 100 ] );

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
                    return y( d.memberAverageLoadAverage );
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
                    result += y( data.memberAverageLoadAverage ) - detailHeight - detailMargin;
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
            var text = details.append( 'text' )
                .attr( 'class', 'lineChart--bubble--text' );
            text.append( 'tspan' )
                .attr( 'class', 'lineChart--bubble--label' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 3 )
                .attr( 'text-anchor', 'middle' )
                .text( timeConverter(data.timeStamp/1000) );

            text.append( 'tspan' )
                .attr( 'class', 'lineChart--bubble--value' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 4 * 3 )
                .attr( 'text-anchor', 'middle' )
                .text( data.memberAverageLoadAverage );

        }

    }

    var data = jsonDataLoadAvg;
    drawLineChart('chart2',data);


}

function inFlightRequestCountChart () {

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
                .attr( 'class', 'lineChart--bubble--label' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 3 )
                .attr( 'text-anchor', 'middle' )
                .text( timeConverter(data.timeStamp/1000) );


            text.append( 'tspan' )
                .attr( 'class', 'lineChart--bubble--value' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 4 * 3 )
                .attr( 'text-anchor', 'middle' )
                .text( data.inFlightRequestCount );

        }

    }

    var data = jsonDataFlightRequest;
    drawLineChart('chart3',data);

}


function memoryAverageDataChart () {


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
                .y1( function( d ) { return y( d.memberAverageMemoryConsumption ); } ),

            line = d3.svg.line()
                .interpolate( 'linear' )
                .x( function( d ) { return x( d.timeStamp ) + detailWidth / 2; } )
                .y( function( d ) { return y( d.memberAverageMemoryConsumption ); } ),

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
        y.domain( [ 0, d3.max( data, function( d ) { return d.memberAverageMemoryConsumption; } ) + 100 ] );

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
                    return y( d.memberAverageMemoryConsumption );
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
                    result += y( data.memberAverageMemoryConsumption ) - detailHeight - detailMargin;
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

            var text = details.append( 'text' )
                .attr( 'class', 'lineChart--bubble--text' );
            text.append( 'tspan' )
                .attr( 'class', 'lineChart--bubble--label' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 3 )
                .attr( 'text-anchor', 'middle' )
                .text( timeConverter(data.timeStamp/1000) );

            text.append( 'tspan' )
                .attr( 'class', 'lineChart--bubble--value' )
                .attr( 'x', detailWidth / 2 )
                .attr( 'y', detailHeight / 4 * 3 )
                .attr( 'text-anchor', 'middle' )
                .text( data.memberAverageMemoryConsumption );

        }

    }

    var data = jsonDataMemoryAvg;
    drawLineChart('chart1', data);


}


function timeConverter(timestamp){

    var a = new Date(timestamp * 1000);
    var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    var year = a.getFullYear();
    var month = months[a.getMonth()];
    var date = a.getDate();
    var hour = a.getHours();
    var min = a.getMinutes();
    var sec = a.getSeconds();
    var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;
    return time;
}

function tween( b, callback ) {
    return function( a ) {
        var i = d3.interpolateArray( a, b );

        return function( t ) {
            return callback( i ( t ) );
        };
    };
}

