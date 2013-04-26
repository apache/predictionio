var AlgoSettingsModel = Backbone.Model.extend({
	/* Required params: app_id, engine_id, id (algo_id) */
	urlRoot: function(){ 
		return '/modules/itemrec/settings/app/'+ this.get("app_id") +'/engine/' + this.get("engine_id") + '/mahout-alswr';
	}
});

var AlgoSettingsView = Backbone.View.extend({
    el: '#algoSettingsContentHolder', 
    initialize : function() {
    	this.form_el = '#algoSettingsForm';
        this.template = _.template($("#algoSettingsTemplate").html());
		this.app_id = this.options.app_id;
		this.engine_id = this.options.engine_id;
		this.algo_id = this.options.algo_id;
		this.algotype_id = this.options.algotype_id;
		this.model = new AlgoSettingsModel({app_id: this.app_id, engine_id: this.engine_id, id: this.algo_id})
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
				//
				self.initValue('tune');
				self.initValue('tuneMethod');
				self.initValue('numFeaturesMin');
				self.initValue('numFeaturesMax');
				self.initValue('lambdaMin');
				self.initValue('lambdaMax');
				self.initValue('numIterationsMin');
				self.initValue('numIterationsMax');
				//
				if (self.model.get('tune') == 'auto') {
					self.tuneAuto();
				}

			}
		});
    },
    initValue: function(attrName){
		var value = this.model.get(attrName);
		this.$el.find('#'+attrName).val(value);
    },
	events: {
		"submit #algoSettingsForm" : "formDataSubmit",
		'click #tuneManual' : "tuneManual", 
		'click #tuneAuto' : "tuneAuto"
	},
    render : function() {
        this.$el.html(this.template());
        return this;
    },
	reloadData : function() { // Required Algorithm Module Function
	},
	tuneManual: function() {
		$('#tuneAuto').removeAttr('checked');
		$('#tuneManual').attr('checked', 'checked');
		$('#tuneAutoPanel').slideUp(); 
		$('#tuneManualPanel').slideDown();
	},
	tuneAuto: function() {
		$('#tuneManual').removeAttr('checked');
		$('#tuneAuto').attr('checked', 'checked');
		$('#tuneManualPanel').slideUp(); 
		$('#tuneAutoPanel').slideDown();
	},
	formDataSubmit: function() {
		var data = formToJSON(this.$el.find(this.form_el)); // convert form names/values of fields into key/value pairs
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

createAlgorithmView = function(app_id, engine_id, algo_id, algotype_id) { // Required Algorithm Module Function
    return new AlgoSettingsView({app_id: app_id, engine_id: engine_id, algo_id: algo_id, algotype_id: algotype_id});
};