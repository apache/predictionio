var PdioRandomRankAlgoSettingsView = Backbone.View.extend({
    el: '#pdio-randomrankContentHolder', 
    initialize : function() {
        this.template = _.template($("#pdio-randomrankTemplate").html());
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

createAlgorithmView = function(app_id, engine_id, algo_id) { // Required Algorithm Module Function
    return new PdioRandomRankAlgoSettingsView();
};