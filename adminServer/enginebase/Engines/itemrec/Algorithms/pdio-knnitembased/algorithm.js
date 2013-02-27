var PdioKnnItemBasedAlgoSettingsModel = Backbone.Model.extend({
	/* Required params: app_id, engine_id, id (algo_id) */
	urlRoot: function(){ 
		return '/modules/itemrec/settings/app/'+ this.get("app_id") +'/engine/' + this.get("engine_id") + '/pdio-knnitembased';
	}
});

var PdioKnnItemBasedAlgoSettingsView = Backbone.View.extend({
    el: '#pdio-knnitembasedContentHolder', 
    initialize : function() {
    	this.form_el = '#pdio-knnitembasedForm';
        this.template = _.template($("#pdio-knnitembasedTemplate").html());
		this.app_id = this.options.app_id;
		this.engine_id = this.options.engine_id;
		this.algo_id = this.options.algo_id;
		this.model = new PdioKnnItemBasedAlgoSettingsModel({app_id: this.app_id, engine_id: this.engine_id, id: this.algo_id})
		var self = this;
		this.model.fetch({
			success: function() {
				self.render();
				self.initValue('distanceFunc');
				self.initValue('virtualCount');
				self.initValue('priorCorrelation');
				self.initValue('minNumRaters');
				self.initValue('maxNumRaters');
				self.initValue('minIntersection');
				self.initValue('minNumRatedSim');
				self.initValue('viewAction');
				self.initValue('viewmoreAction');
				self.initValue('likeAction');
				self.initValue('dislikeAction');
				self.initValue('buyAction');
				self.initValue('override');
			}
		});
    },
    initValue: function(attrName){
		var value = this.model.get(attrName);
		this.$el.find('#knnitembase_'+attrName).val(value);
    },
	events: {
		"change #pdio-knnitembasedForm input":  "formDataChanged",
		"change #pdio-knnitembasedForm select":  "formDataChanged"
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
    return new PdioKnnItemBasedAlgoSettingsView({app_id: app_id, engine_id: engine_id, algo_id: algo_id});
};