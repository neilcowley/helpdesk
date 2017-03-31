$(function() {
	$('.qtyOrdered').click(function(){ORDER_DETAILS.clear(this)}); 
	$('.datePicker').click(function(){ORDER_DETAILS.datePickerOpen(this)});
	$('.checkUserName').blur(function(){USER.checkUserName() }); 
	//$('.emailSelect').click(function(){SUPPORT.toggleEmailSelect(this)});
	
	$(document)
	//	.on('click','.emailSelect', function(e){SUPPORT.toggleEmailSelect(this)})
	
});


var USER = USER || {};

USER = {
	checkUserName : function(){

		var fullName = $('.fullName');
		if(fullName[0].value==""){
			var fName = $('.fName')[0].value;
			var lName = $('.lName')[0].value;
			if(fName != "" && lName != "" ){
				fullName[0].value= fName + " " + lName;
			}
		}
	}	
}

var ORDER_DETAILS = ORDER_DETAILS || {};

ORDER_DETAILS = {
	clear : function(el){
		if(el.value=="0"){
			el.value="";
		}
	},
	datePickerOpen: function(el){
		$(el).datetimepicker({
			 pickDate: true,
		     pickTime: false,
		     showToday: true
	    });
	}
}

var SUPPORT = SUPPORT || {}; 

SUPPORT = {
	
	toggleEmailSelect : function(el){
		// 
		//$(el).toggleClass("emailSelected");
		// $(el).addClass("emailSelected");
		//var node = el.childNodes[0];
		//alert("inside=" + node.textContent);
	},
	/** Called when an upload is instigated. Sets the total number of files to be uploaded into a variable */
	addNumberOfFilesToUpload : function(fileupload){
		this.importFileNumber = fileupload.files.length ; 
		//console.log("TOTAL NUMBER OF FILES=" + this.importFileNumber );
		//$('#callDetails\\:file_number_input').val(fileupload.files.length);
		
	}, 
	/** Called each time a file is uploaded and checks how many are left. If all files
	 *  have been uploaded a remoteCommand is called which triggers a save.  */
	attachAndSave : function(fileupload){
		// After all uploads are complete, call a save on the call so that the uploads are uploaded.
		this.importFileNumber--; 
		// console.log("NUMBER OF FILES LEFT=" + this.importFileNumber );
		if (this.importFileNumber == 0){
			rc();
		}
	}
}