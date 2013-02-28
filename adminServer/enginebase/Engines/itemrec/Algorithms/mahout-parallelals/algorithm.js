var MahoutParallelALSAlgoSettingsModel = Backbone.Model.extend({
	/* Required params: app_id, engine_id, id (algo_id) */
	urlRoot: function(){ 
		return '/modules/itemrec/settings/app/'+ this.get("app_id") +'/engine/' + this.get("engine_id") + '/mahout-parallelals';
	}
});

var MahoutParallalALSAlgoSettingsView = Backbone.View.extend({
    el: '#mahout-parallelalsContentHolder', 
    initialize : function() {
    	this.form_el = '#mahout-parallelalsForm';
        this.template = _.template($("#mahout-parallelalsTemplate").html());
		this.app_id = this.options.app_id;
		this.engine_id = this.options.engine_id;
		this.algo_id = this.options.algo_id;
		this.model = new MahoutParallelALSAlgoSettingsModel({app_id: this.app_id, engine_id: this.engine_id, id: this.algo_id})
		var self = this;
		this.model.fetch({
			success: function() {
				self.render();
				/* TODO init parallel als values;
				self.initValue('similarityClassname');
				self.initValue('viewParam');
				self.initValue('likeParam');
				self.initValue('dislikeParam');
				self.initValue('conversionParam');
				self.initValue('conflictParam');
				*/
			}
		});
    },
    initValue: function(attrName){
		var value = this.model.get(attrName);
		this.$el.find('#mahout-parallelals_'+attrName).val(value);
    },
	events: {
		"change #mahout-parallelalsForm input":  "formDataChanged",
		"change #mahout-parallelalsForm select":  "formDataChanged"
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
    return new MahoutParallalALSAlgoSettingsView({app_id: app_id, engine_id: engine_id, algo_id: algo_id});
};