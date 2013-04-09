var MahoutALSWRAlgoSettingsModel = Backbone.Model.extend({
	/* Required params: app_id, engine_id, id (algo_id) */
	urlRoot: function(){ 
		return '/modules/itemrec/settings/app/'+ this.get("app_id") +'/engine/' + this.get("engine_id") + '/mahout-alswr';
	}
});

var MahoutALSWRAlgoSettingsView = Backbone.View.extend({
    el: '#mahout-alswrContentHolder', 
    initialize : function() {
    	this.form_el = '#mahout-alswrForm';
        this.template = _.template($("#mahout-alswrTemplate").html());
		this.app_id = this.options.app_id;
		this.engine_id = this.options.engine_id;
		this.algo_id = this.options.algo_id;
		this.model = new MahoutALSWRAlgoSettingsModel({app_id: this.app_id, engine_id: this.engine_id, id: this.algo_id})
		var self = this;
		this.model.fetch({
			success: function() {
				self.render();
				self.initValue('numFeatures');
				self.initValue('lambda');
				self.initValue('numIterations');
				self.initValue('viewParam');
				self.initValue('likeParam');
				self.initValue('dislikeParam');
				self.initValue('conversionParam');
				self.initValue('conflictParam');
				
				// TODO: If Autotune is set, toggleTune
				// TODO: load Autotune values
			}
		});
    },
    initValue: function(attrName){
		var value = this.model.get(attrName);
		this.$el.find('#mahout-alswr_'+attrName).val(value);
    },
	events: {
		//"change #mahout-alswrForm input":  "formDataChanged",
		//"change #mahout-alswrForm select":  "formDataChanged",
		"submit #mahout-alswrForm" : "formDataSubmit",
		'click input[name="tune"]' : "toggleTune" 
	},
    render : function() {
        this.$el.html(this.template());
        return this;
    },
	reloadData : function() { // Required Algorithm Module Function
	},
	toggleTune: function() {
	     $('#tuneManualPanel').slideToggle();
	     $('#tuneAutoPanel').slideToggle(); 
	},
	formDataSubmit: function() {
		var data = formToJSON(this.$el.find(this.form_el)); // convert form names/values of fields into key/value pairs
		console.log(data);
		this.model.save(data, {
			wait: true,
			success: function(model, res) {
				window.location.hash = 'engineTabAlgorithms';
			},
			error: function(model, res){
				alert("An error has occured. HTTP Status Code: "
						+ res.status);
			}
		});
		return false;
	},
    close : function() {  // Required Algorithm Module Function
        this.remove();
        this.off();
        // handle other unbinding needs, here
        _.each(this.subViews, function(subView){
            if (subView.close){
                subView.close();
            }
        });
    }
});

createAlgorithmView = function(app_id, engine_id, algo_id) { // Required Algorithm Module Function
    return new MahoutALSWRAlgoSettingsView({app_id: app_id, engine_id: engine_id, algo_id: algo_id});
};