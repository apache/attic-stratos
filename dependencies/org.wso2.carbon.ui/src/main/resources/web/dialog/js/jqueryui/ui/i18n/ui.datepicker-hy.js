/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* Armenian(UTF-8) initialisation for the jQuery UI date picker plugin. */
/* Written by Levon Zakaryan (levon.zakaryan@gmail.com)*/
jQuery(function($){
	$.datepicker.regional['hy'] = {
		clearText: 'Մաքրել', clearStatus: '',
		closeText: 'Փակել', closeStatus: '',
		prevText: '&#x3c;Նախ.',  prevStatus: '',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'Հաջ.&#x3e;', nextStatus: '',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'Այսօր', currentStatus: '',
		monthNames: ['Հունվար','Փետրվար','Մարտ','Ապրիլ','Մայիս','Հունիս',
		'Հուլիս','Օգոստոս','Սեպտեմբեր','Հոկտեմբեր','Նոյեմբեր','Դեկտեմբեր'],
		monthNamesShort: ['Հունվ','Փետր','Մարտ','Ապր','Մայիս','Հունիս',
		'Հուլ','Օգս','Սեպ','Հոկ','Նոյ','Դեկ'],
		monthStatus: '', yearStatus: '',
		weekHeader: 'ՇԲՏ', weekStatus: '',
		dayNames: ['կիրակի','եկուշաբթի','երեքշաբթի','չորեքշաբթի','հինգշաբթի','ուրբաթ','շաբաթ'],
		dayNamesShort: ['կիր','երկ','երք','չրք','հնգ','ուրբ','շբթ'],
		dayNamesMin: ['կիր','երկ','երք','չրք','հնգ','ուրբ','շբթ'],
		dayStatus: 'DD', dateStatus: 'D, M d',
		dateFormat: 'dd.mm.yy', firstDay: 1, 
		initStatus: '', isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['hy']);
});