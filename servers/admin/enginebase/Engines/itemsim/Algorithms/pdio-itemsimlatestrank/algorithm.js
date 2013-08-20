var AlgoSettingsView = Backbone.View.extend({
    el: '#algoSettingsContentHolder', 
    initialize : function() {
        this.template = _.template($("#algoSettingsTemplate").html());
		this.render();
    },
    render : function() {
        this.$el.html(this.template());
        return this;
    },
	reloadData : function() { // Required Algorithm Module Function
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
    return new AlgoSettingsView();
};