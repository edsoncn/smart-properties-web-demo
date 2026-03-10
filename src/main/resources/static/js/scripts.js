/*!
* Start Bootstrap - Shop Homepage v5.0.4 (https://startbootstrap.com/template/shop-homepage)
* Copyright 2013-2021 Start Bootstrap
* Licensed under MIT (https://github.com/StartBootstrap/startbootstrap-shop-homepage/blob/master/LICENSE)
*/
// This file is intentionally blank
// Use this file to add JavaScript to your project

function updateVariable() {
    var array = document.getElementById('form-variables'); // Encodes the set of form elements as an array of names and values.
    var json = {};

    for(var entry of array){
        if(entry.name !== ''){
            json[entry.name] = entry.value || "";
        }
    }
    $.ajax({
        url: array.action,
        type: "POST",
        data: JSON.stringify(json),
        processData: false,
        contentType: "application/json; charset=UTF-8",
        complete: function(result){
            location.reload();
        }
    });
}