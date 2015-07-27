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

/* Turkish initialisation for the jQuery UI date picker plugin. */
/* Written by Izzet Emre Erkan (kara@karalamalar.net). */
jQuery(function($){
	$.datepicker.regional['tr'] = {
		clearText: 'temizle', clearStatus: 'geçerli tarihi temizler',
		closeText: 'kapat', closeStatus: 'sadece göstergeyi kapat',
		prevText: '&#x3c;geri', prevStatus: 'önceki ayı göster',
		prevBigText: '&#x3c;&#x3c;', prevBigStatus: '',
		nextText: 'ileri&#x3e', nextStatus: 'sonraki ayı göster',
		nextBigText: '&#x3e;&#x3e;', nextBigStatus: '',
		currentText: 'bugün', currentStatus: '',
		monthNames: ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran',
		'Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'],
		monthNamesShort: ['Oca','Şub','Mar','Nis','May','Haz',
		'Tem','Ağu','Eyl','Eki','Kas','Ara'],
		monthStatus: 'başka ay', yearStatus: 'başka yıl',
		weekHeader: 'Hf', weekStatus: 'Ayın haftaları',
		dayNames: ['Pazar','Pazartesi','Salı','Çarşamba','Perşembe','Cuma','Cumartesi'],
		dayNamesShort: ['Pz','Pt','Sa','Ça','Pe','Cu','Ct'],
		dayNamesMin: ['Pz','Pt','Sa','Ça','Pe','Cu','Ct'],
		dayStatus: 'Haftanın ilk gününü belirleyin', dateStatus: 'D, M d seçiniz',
		dateFormat: 'dd.mm.yy', firstDay: 1, 
		initStatus: 'Bir tarih seçiniz', isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['tr']);
});