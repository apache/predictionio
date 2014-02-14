@(engineInfoId: String, params: Seq[io.prediction.commons.settings.Param])
var EngineSettingsModel = Backbone.Model.extend({
    /* Required params: app_id, id (engine_id) */
    urlRoot: function(){
        return '/apps/' + this.get("app_id") + '/engine_settings';
    },
    /* Override save for displaying saving status */
    save: function(attributes, options) {
        var settingSave = toastr.info('Saving Settings...','', {positionClass: 'toast-bottom-right'});
        var result = Backbone.Model.prototype.save.call(this, attributes, options);
        toastr.clear(settingSave);
        return result;
    }
});

var EngineSettingsView = Backbone.View.extend({
    el: '#engineContentHolder',
    initialize : function() {
        this.subViews = []; // keep track of sub view
        this.template = _.template($("#engineTemplate").html());
        this.index = 0;
        this.engine_id = this.options.engine_id;
        this.app_id = this.options.app_id;
        this.itemtypelist = {}; // storing itemtypes
        var self = this;
        this.model = new EngineSettingsModel({app_id: this.app_id, id: this.engine_id, infotype: "engine", infoid: this.options.engineinfo_id});
        this.model.fetch({
            success: function() {
                self.render();

                // load itemtypes to this.itemtypelist and display it
                var currItemTypeList = self.model.get('itemtypelist');
                if (currItemTypeList) {
                    for (var i=0;i < currItemTypeList.length; i++) {
                        var currItemtype_id = currItemTypeList[i];
                        self.itemtypelist[currItemtype_id] = true;
                        self.addItemTypeView(currItemtype_id);
                    }
                }

            @for(p <- params) {
                self.initValue('@p.id');
            }
            }
        });
    },
    initValue: function(attrName){
        var value = this.model.get(attrName);
        this.$el.find('#'+attrName).val(value);
    },
    events : {
        "click #engineAddItemTypeBtn" : "addItemType",
        'keypress #engineAddItemTypeInput': 'onEnterAddItemType',
        @for(p <- params) {
            @if(p.ui.uitype != "slider") {
        "change #@(p.id)": "@(p.id)Changed",
            }
        }
        "change #trainingdisabled" : "trainingdisabledChanged",
        "change #trainingschedule" : "trainingscheduleChanged",
        "change #engineAllItemTypes" : "toggleAllItemTypes"
    },
    onEnterAddItemType : function(e) {
        if (e.keyCode == 13) { // if it's ENTER
            this.addItemType();
            return false;
        } else { // continue if it's not ENTER
            return true;
        }
    },
    addItemType : function() {
        var inputObj = this.$el.find('#engineAddItemTypeInput');
        var itemtype_id = inputObj.val();
        // add itemtype
        this.itemtypelist[itemtype_id] = true;
        this.model.set({
            itemtypelist: MapKeyToArray(this.itemtypelist),
            allitemtypes: false
        });
        var self = this;
        this.model.save({},{
            success: function(model, res) {
                self.addItemTypeView(itemtype_id);
                inputObj.val(''); // clear input field
                self.$el.find('#engineAllItemTypes').attr('checked', false); // unselect include all
            }
        });
        return false;
    },
    addItemTypeView: function(itemtype_id){
        var itemTypeView = new EngineSettingsItemTypeView({ itemtype_id: itemtype_id, index: this.index});
        this.$el.find('#engineItemTypeList_ContentHolder').append(itemTypeView.render().el);
        this.subViews.push(itemTypeView);
        this.listenTo(itemTypeView, 'ItemTypeRemoved', this.itemtypeRemoved);
        itemTypeView.listenTo(this, 'AllItemTypesSelected', itemTypeView.remove);
        this.index += 1;
    },
    itemtypeRemoved: function(itemtype_id) {
        if (itemtype_id in this.itemtypelist) {
            delete this.itemtypelist[itemtype_id];
            if ($.isEmptyObject(this.itemtypelist)) { // if no more selected item types
                this.model.set({allitemtypes: true});
                this.$el.find('#engineAllItemTypes').prop('checked', true);
            }
            this.model.set({itemtypelist: MapKeyToArray(this.itemtypelist)});
            this.model.save();
        }
    },
    toggleAllItemTypes: function() {
        var inputObj = this.$el.find('#engineAllItemTypes');
        var isAllItemTypes = inputObj.is(':checked');
        if (isAllItemTypes == true) {   // select AllItemTypes
            this.trigger('AllItemTypesSelected');
            this.itemtypelist = {};
            this.model.set({itemtypelist: MapKeyToArray(this.itemtypelist), allitemtypes: true});
            this.model.save();
        } else { //unselect AllItemTypes
            createDialog('Item Type Required','You must select at least one item type for the engine.', {
                  resizable: false,
                  height:185,
                  modal: false,
                  buttons: {
                    Okay: function() {
                        $( this ).dialog( "close" );
                    }
                  }
            });
            inputObj.prop('checked', true); // disallow unselect ALlItemTypes manually
        }
    },
    trainingdisabledChanged: function(e) {
        var trainingdisabled = this.$el.find('#trainingdisabled').is(':checked');
        this.model.set({trainingdisabled: trainingdisabled});
        this.model.save();
        return false;
    },
    trainingscheduleChanged: function(e) {
        var trainingschedule = this.$el.find('#trainingschedule').val();
        this.model.set({trainingschedule: trainingschedule});
        this.model.save();
        return false;
    },
    @for(p <- params) {
        @if(p.ui.uitype != "slider") {
    @(p.id)Changed: function(e) {
        var @p.id = this.$el.find('#@p.id').val();
        this.model.set({@p.id: @p.id});
        this.model.save();
        return false;
    },
        }
    }
    render : function() {
        this.$el.html(this.template({'data': this.model.toJSON()}));
        var self = this;
        @for(p <- params) {
            @if(p.ui.uitype == "slider") {
        var @p.id = self.model.get('@p.id');

        this.$el.find("#slider-@p.id").slider({
            value : @p.id,
            min : @p.ui.slidermin.get,
            max : @p.ui.slidermax.get,
            step : @p.ui.sliderstep.get,
            slide : function(event, ui) {
                self.model.set({@p.id: ui.value});
                self.model.save({}, {success: function(){
                    self.$el.find("#slider-@p.id-val").text(ui.value);
                }});
            }
        });
        this.$el.find("#slider-@p.id-val").text(@p.id);
            }
        }
        return this;
    },
    reloadData : function() { // Required Engine Module Function
    },
    close : function() {  // Required Engine Module Function
        try {
            @for(p <- params) {
                @if(p.ui.uitype == "slider") {
            this.$el.find("#slider-@p.id").slider("destroy");
                }
            }
        } catch(e){};

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

/* Required Param: itemtype_id, index*/
var EngineSettingsItemTypeView = Backbone.View.extend({
    tagName: 'tr',
    initialize: function(){
        this.template_el = '#engineItemTypeList_template';
        this.template = _.template($(this.template_el).html()); // define template function
        this.itemtype_id = this.options.itemtype_id;
        this.index  = this.options.index;
    },
    events : {
        "click .removeItemTypeBtn" : "removeItemType"
    },
    render: function(){
        //this.$el.html( this.template({"data": this.model.toJSON()}) );
        this.$el.html( this.template({"data": {
            itemtype_id: this.itemtype_id,
            index: this.index
        }}) );
        return this;
    },
    removeItemType: function() {
        this.remove();
        this.trigger('ItemTypeRemoved', this.itemtype_id);
        return false;
    }
});

var createEngineView = function(app_id, engine_id, engineinfo_id) { // Required Engine Module Function
    return new EngineSettingsView({app_id: app_id, engine_id: engine_id, engineinfo_id: engineinfo_id});
};
