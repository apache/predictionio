var MahoutThresholdUserBasedAlgoSettingsModel = Backbone.Model.extend({
	/* Required params: app_id, engine_id, id (algo_id) */
	urlRoot: function(){ 
		return '/modules/itemrec/settings/app/'+ this.get("app_id") +'/engine/' + this.get("engine_id") + '/mahout-thresholduserbased';
	}
});

var MahoutThresholdUserBasedAlgoSettingsView = Backbone.View.extend({
    el: '#mahout-thresholduserbasedContentHolder', 
    initialize : function() {
    	this.form_el = '#mahout-thresholduserbasedForm';
        this.template = _.template($("#mahout-thresholduserbasedTemplate").html());
		this.app_id = this.options.app_id;
		this.engine_id = this.options.engine_id;
		this.algo_id = this.options.algo_id;
		this.model = new MahoutThresholdUserBasedAlgoSettingsModel({app_id: this.app_id, engine_id: this.engine_id, id: this.algo_id})
		var self = this;
		this.model.fetch({
			success: function() {
				self.render();
				self.initValue('userSimilarity');
				self.initValue('threshold');
				self.initValue('booleanData');
				self.initValue('weighted');
				self.initValue('samplingRate');
				self.initValue('viewParam');
				self.initValue('likeParam');
				self.initValue('dislikeParam');
				self.initValue('conversionParam');
				self.initValue('conflictParam');
			}
		});
    },
    initValue: function(attrName){
		var value = this.model.get(attrName);
		this.$el.find('#mahout-thresholduserbased_'+attrName).val(value);
    },
	events: {
		"change #mahout-thresholduserbasedForm input":  "formDataChanged",
		"change #mahout-thresholduserbasedForm select":  "formDataChanged"
	},
    render : function() {
        this.$el.html(this.template());
        return this;
    },
	reloadData : function() { // Required Algorithm Module Function
	},
	formDataChanged: function() {
		var data = formToJSON(this.$el.find(this.form_el)); // convert form names/values of fields into key/value pairs
		console.log(data);
		this.model.set(data);
		this.model.save();
		/*
		var simEvalModel = new SimEvalModel({app_id: this.app_id, engine_id: this.engine_id});
		var self = this;
		simEvalModel.save(data, {
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
		*/
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
    return new MahoutThresholdUserBasedAlgoSettingsView({app_id: app_id, engine_id: engine_id, algo_id: algo_id});
};