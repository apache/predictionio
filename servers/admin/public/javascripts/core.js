var ADMIN_API_PATH = '/';
function getAPIUrl(path) {
	return ADMIN_API_PATH + path;
}

/* TO BE DISCUSSED: whether we want to use default 'id' or mongo's '_id'
 * Backbone.Model.prototype.idAttribute = "_id";
 */

/* Function to convert form's name/value pairs to JSON */
function formToJSON(formObj) {
	var arr = formObj.serializeArray();
	var data = _(arr).reduce(function(acc, field) {
		acc[field.name] = field.value;
		return acc;
	}, {});
	return data;
}
function getUrlParam(key) {
	var result = new RegExp(key + "=([^&]*)", "i").exec(window.location.search);
	return result && decodeURIComponent(result[1]) || "";
}
/* URI encode keys of map, separated by , */
function encodeURIComponentMapKey(jmap) {
	  var array = [];
	  for (var key in jmap)
		  array.push(encodeURIComponent(key));
	  return array.join(',');
}
function MapKeyToArray(mapObj) {
	var array = [];
	for (var key in mapObj) {
	    if (mapObj.hasOwnProperty(key)) {
	        array.push(key);
	    }
	}
	return array;
}
/* Global Dialogbox function */
function createDialog(title, content, params) {
	$('#dialog_template').attr('title',title).find('#dialog_text').text(content).end().dialog(params);
}

function createDialogErrorResponse(res) {
	var message = "HTTP " + res.status + ". ";
	try { // show error message if fail
		var resData = $.parseJSON(res.responseText);
		message = message + resData.message;
	} catch (err) {
		message = message + "An error has occured.";
	}
	createDialog("", message, {
    	width: 400,
    	modal: true
	});
}

/* Notification functions */
function notifyInfo(message, title, optionsOverride) {
	return toastr.info(message, title, optionsOverride);
}

function notifySuccess(message, title, optionsOverride) {
	return toastr.success(message, title, optionsOverride);
}

function notifyError(message, title, optionsOverride) {
	return toastr.error(message, title, optionsOverride);
}

function notifyErrorSticky(message, title) {
	return toastr.error(message, title, {positionClass: 'toast-bottom-right', timeOut: 0, extendedTimeOut: 0});
}

function notifyInfoDefault(message, title) {
	return toastr.info(message, title, {positionClass: 'toast-bottom-right', timeOut: 4000});
}

function notifyInfoSticky(message, title) {
	return toastr.info(message, title, {positionClass: 'toast-bottom-right', timeOut: 0, extendedTimeOut: 0, fadeOut: 1});
}

function notifyClear(element) {
	return toastr.clear(element);
}

function notifyErrorResponse(res) {
	var message = "HTTP " + res.status + ". ";
	try { // show error message if fail
		var resData = $.parseJSON(res.responseText);
		message = message + resData.message;
	} catch (err) {
		message = message + "An error has occured.";
	}
	notifyErrorSticky(message, "");
}

$(function() {
	// TODO: prevent "a href='#'" behavior

	var core_router = new CoreRouter();
	Backbone.history.start();
});

/*
 *  Default Close Methods for Backbone Views
 *  Add custom closing codes to 'beforeClose' method in your View class
 *  Push subviews to this.subViews = [] so that they will be closed automatically (e.g. this.subViews.push(subView);)
 */
Backbone.View.prototype.close = function() {
	if (this.beforeClose) {  // for custom closing codes
		this.beforeClose();
	}
	this.remove();
	this.off();
	if (this.subViews) {  // for closing subviews
		_.each(this.subViews, function(subView){
			if (subView.close){
				subView.close();
			}
		});
	}
}

/* abstract function for engine and algo modules */
var createAlgorithmView = function() { return null; };
var createEngineView = function(appid, engineid) { return null; };

var CoreRouter = Backbone.Router.extend({
	initialize : function() {
		this.target_el = '#ContentHolder';
		this.currentView = null;
		this.auth_el = '#AuthHolder';
		this.authView = new AuthView();
		$(this.auth_el).html(this.authView.render().el);
	},
	routes : {
		signin : 'signinPage',
		signout : 'signout',
		appsDashboard : 'appsDashboardPage',
		engine : 'engineTabSettings',
		engineTabSettings : 'engineTabSettings',
		engineTabAlgorithms : 'engineTabAlgorithms',
		engineAddAlgorithm : 'engineExtraTabAddAlgorithm',
		addEngine : 'addEnginePage',
		'algoSettings/:algoinfoid/:algoName/:algoid' : 'engineExtraTabAlgorithmSettings',
		'algoAutotuningReport/:algoid' : 'engineExtraTabAlgoAutotuningReport',
		'simEvalSettings/:algoidlist' : 'engineExtraTabSimEvalSettings',
		'simEvalReport/:id' : 'engineExtraTabSimEvalReport',
		'*actions' : 'defaultRoute' // default -- exception
	},
	defaultRoute : function(action) { // exception
		var path = window.location.protocol + '//' + window.location.host + window.location.pathname + '#appsDashboard';
		window.location = path;
	},
	signinPage : function() {
		this.authView.clearAuth();
		if (this.currentView) {this.currentView.close();}
		this.currentView = new SigninView();
		$(this.target_el).html(this.currentView.render().el);
	},
	signout: function() {
		$.post(getAPIUrl('signout')).success(function() {
			window.location.hash = 'signin';
		}).error(function(res){
			alert("An error has occured. HTTP Status Code: " + res.status);
		});
		return false;
	},
	appsDashboardPage : function() {
		this.authView.ensureAuth();
		if (this.currentView) {this.currentView.close();}
		this.currentView = new AppsDashboardView();
		$(this.target_el).html(this.currentView.render().el);
	},
	engineTabSettings : function() {
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showTabSettings();
		return false;
	},
	engineTabAlgorithms : function() {
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showTabAlgorithms();
		return false;
	},
	engineExtraTabAddAlgorithm : function() {
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showExtraTabAddAlgorithm();
		return false;
	},
	addEnginePage : function() {
		this.authView.ensureAuth();
		if (this.currentView) {this.currentView.close();}
		this.currentView = new AddEngineView();
		$(this.target_el).html(this.currentView.el);
	},
	engineExtraTabAlgorithmSettings: function(algoinfoid, algoName, algoid) {
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showExtraTabAlgorithmSettings(algoinfoid, algoName, algoid);
	},
	engineExtraTabAlgoAutotuningReport: function(algoid) {
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showExtraTabAlgoAutotuningReport(algoid);
	},
	engineExtraTabSimEvalReport : function(simeval_id) {
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showExtraTabSimEvalReport(simeval_id);
	},
	engineExtraTabSimEvalSettings : function(algoidlist) {
		// TODO: encodeURIComponent each element in the comma-separated algoidlist
		this.authView.ensureAuth();
		if (!this.currentView || !this.currentView.isEngineView) { // if not currently Engine view
			if (this.currentView) {this.currentView.close();}
			this.currentView = new EngineView();
			$(this.target_el).html(this.currentView.render().el);
		}
		this.currentView.showExtraTabSimEvalSettings(algoidlist);
	}
});

var AuthModel = Backbone.Model.extend({
	urlRoot : getAPIUrl('auth')
});
var AuthView = Backbone.View.extend({
    initialize: function(){
    	this.template_el = '#auth_template';
    	this.template = _.template($(this.template_el).html()); // define template function
    	this.isEnsureAuth = false;
    	this.model = new AuthModel(); // assign auth data model
    	this.model.bind('change', this.render, this);
    	this.reloadAuth();
    },
    render: function(){
        this.$el.html( this.template({"data": this.model.toJSON()}) );
        // for v0.2 processing task
        // this.navAuthViewObj = new NavTaskView();
        return this;
    },
    reloadAuth: function() {
    	var self = this; // for accessing "this" in callback func
    	self.model.clear({ silent: true }); // clear previous auth model data
    	this.model.fetch({
    		error: function() {
    			if (self.isEnsureAuth) { // if auth required but auth fails, redirect to #signin
    				self.isEnsureAuth = false;
    				window.location.hash = 'signin';
    			}
    			self.render(); // show no auth view
    		}
    	});
    },
    ensureAuth: function() {
    	this.isEnsureAuth = true;
    	this.reloadAuth();
    },
    clearAuth: function() {
    	this.model.clear();
    }
})


var BreadcrumbView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#breadcrumb_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
	},
	render : function() {
		var data = {};
		$.ajaxSetup({async:false});
		if (this.appid) {
			data['appid'] = this.appid;
			var appModel = new AppModel({id: this.appid});
			appModel.fetch({
				success: function() {
					data['appname'] = appModel.get('appname');
				}
			});
		}
		if (this.engineid) {
			data['engineid'] = this.engineid;
			var engineModel = new EngineModel({appid: this.appid, id: this.engineid});
			engineModel.fetch({
				success: function() {
					data['enginename'] = engineModel.get('enginename');
				}
			});

		}
		if (this.engineinfoid)
			data['engineinfoid'] = this.engineinfoid;
		$.ajaxSetup({async:true});

		this.$el.slideUp().html(this.template({"data": data})).slideDown();
		return this;
	}
});



var SigninView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#signin_template';
		this.error_el = '#signinFormError'; // read error template dom
		this.form_el = '#signinForm';
		this.template = _.template($(this.template_el).html()); // define template function
	},
	events : {
		"submit" : "auth" // bind form submit to auth function
	},
	render : function() {
		this.$el.html(this.template());
		return this;
	},
	auth : function() {
		$(this.error_el).slideUp().html(""); // reset/clear all error msg
		var data = formToJSON(this.$el.find(this.form_el)); // convert form names/values of fields into key/value pairs
		// post to server
		var self = this; // for accessing "this" in callback function
		$.post(getAPIUrl('signin'), data).success(function() {
				window.location.hash = 'appsDashboard';
		}).error(function(res){
			try { // show error message if fail
				var resData = $.parseJSON(res.responseText);
				$(self.error_el).html(resData.message).slideDown("fast");
			} catch (err) {
				alert("An error has occured. HTTP Status Code: "
						+ res.status);
			}
		});
		return false;
	}
})

var AppsDashboardView = Backbone.View.extend({
	initialize: function(){
		this.template_el = '#appsDashboard_template';
		this.template = _.template($(this.template_el).html());
		this.subViews = [];
		this.appListView = new AppListView();
		this.subViews.push(this.appListView); // for auto subview closing
		this.listenTo(this.appListView, 'NoApp', this.showAddApp); // show AddApp area if there's no existing app
	},
	render : function() {
		this.$el.html(this.template());
		this.$el.find('#appList').html(this.appListView.render().el);
		return this;
	},
	events: {
		"click #showAddAppBtn": "showAddApp",
		"click #createAppBtn": "createApp"
	},
	showAddApp: function() {
		this.$el.find("#appCreatePanel").slideDown("fast");
		this.$el.find("#showAddAppBtn").attr("disabled", "true");
		return false;
	},
	hideAddApp: function() {
		var self = this;
		this.$el.find("#appCreatePanel").slideUp("fast", function(){
			self.$el.find("#addAppInputError").hide().html(""); // reset error message
			self.$el.find("#addAppInput").val(""); // clearn input value
			self.$el.find("#showAddAppBtn").removeAttr("disabled");
		});
		return false;
	},
	createApp: function() {
		this.$el.find("#addAppInputError").html(""); // reset error msg
		var appName = this.$el.find("#addAppInput").val();
		var self= this;
		this.appListView.collection.create({
			"appname": appName
		}, {
			wait: true, // wait for server to return the new app info
			success: function() {
				self.hideAddApp();
			},
			error: function(model, res) {
				try { // show error message if fail
					var resData = $.parseJSON(res.responseText);
					$("#addAppInputError").html(resData.message).slideDown("fast");
				} catch(err) {
					alert("An error has occured. HTTP Status Code: " + res.status);
				}
			}
		});
		return this;
	}
});

var AppModel = Backbone.Model.extend({
	urlRoot: getAPIUrl('apps')
});
var AppListModel = Backbone.Collection.extend({
	model: AppModel,
	url: getAPIUrl('apps')
});
var AppListView = Backbone.View.extend({
    initialize: function(){
		this.subViews = [];
    	this.collection = new AppListModel();
    	this.collection.bind('add', this.addOne, this);
    	this.collection.bind('reset', this.render, this);
    	this.collection.fetch();
    },
	render: function(eventData) {
		if (eventData) {
			this.$el.html(""); // clear previous html
			_.each(eventData.models, function(appModel){
				this.addOne(appModel); // debug: console.log(app.attributes['id']);
			}, this);
			if (eventData.models.length == 0) {
				this.trigger('NoApp');
			}
		}
		return this;
	},
    addOne: function(appModel) {
        var appView = new AppView({ model:appModel}); // create app view
		this.subViews.push(appView); // for auto subview closing
        $(appView.render().el).css("display", "none").appendTo(this.$el).slideDown("fast");
    }
});

var AppView = Backbone.View.extend({
	/*
	 * Required Param: model
	 */
	initialize: function(){
		this.id = this.model.get("id"); // app id
		this.template_el = "#app_template";
		this.template = _.template($(this.template_el).html());
		this.subViews = [];
		this.showdetails = false;
	},
	events: {
		"click .toggleAppDetailsAction":  "toggleDetails",
		"click .eraseAllDataAction":  "eraseAllData",
		"click .removeAppAction":  "removeApp"
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		return this;
	},
	toggleDetails: function() {
		if (this.showdetails) {
			this.showdetails = false;
			this.hideDetails();
		} else {
			this.showdetails = true;
			this.showDetails();
		}
		return false;
	},
	showDetails: function() {
		// show app details
		this.appDetailsView = new AppDetailsView({"id": this.id});
		this.subViews.push(this.appDetailsView);
		this.$el.find(".appdetails").html( this.appDetailsView.render().el );
		// show app engine list
		this.appEnginelistView = new AppEnginelistView({"id": this.id});
		this.subViews.push(this.appEnginelistView);
		this.$el.find(".appenginelist").html( this.appEnginelistView.render().el );

		this.$el.find(".app-info").slideDown("fast"); // make app details area visible
	},
	hideDetails: function() {
		var self = this;
		this.$el.find(".app-info").slideUp("fast", function(){ // make app details area hidden
			self.appDetailsView.close();
			self.appEnginelistView.close();
		});
	},
	eraseAllData: function() {
		var self = this;
		createDialog('Erase All Data?','All data of this application will be permanently erased and cannot be recovered. Are you sure?', {
		      resizable: false,
		      height:185,
		      modal: true,
		      buttons: {
		        "Erase All Data": function() {
		        	var erasingInfo = notifyInfoSticky('Erasing All Application Data...','');
		    		$.post(getAPIUrl("apps/"+self.id+"/erase_data"), function() {
		    			self.appDetailsView.model.fetch(); // refresh data
		    			notifyClear(erasingInfo);
		    			notifyInfoDefault('Data Erased.','');
		    		}).error(function(res) {
		    			notifyClear(erasingInfo);
		    			notifyErrorResponse(res);
		    		});
		    		$( this ).dialog( "close" );
		        },
		        Cancel: function() {
		        	$( this ).dialog( "close" );
		        }
		      }
		});
		return false;
	},
	removeApp: function() {
		var self = this;
		createDialog('Remove Application?','This application and all its data will be permanently deleted and cannot be recovered. Are you sure?', {
		      resizable: false,
		      height:185,
		      modal: true,
		      buttons: {
		        "Delete Application": function() {
		        	var deletingInfo = notifyInfoSticky('Deleting Application...','');
		    		self.model.destroy({
		    			success: function(model, res) {
		    				notifyClear(deletingInfo);
		    				notifyInfoDefault('Application Deleted.','');
		    				self.close();
		    			},
		    			error: function(model, res) {
		    				notifyClear(deletingInfo);
		    				notifyErrorResponse(res);
		    			}
		    		});
		    		$( this ).dialog( "close" );
		        },
		        Cancel: function() {
		        	$( this ).dialog( "close" );
		        }
		      }
		});
		return false;
	}
});

var AppDetailsModel = Backbone.Model.extend({
	url: function() {
		return getAPIUrl('apps/' + this.id + "/details");
	}
});
var AppDetailsView = Backbone.View.extend({
	/*
	 * Required Param: id  (app id)
	 */
	initialize: function(){
		this.id = this.options.id; // app id
		this.model = new AppDetailsModel({"id": this.id});
		this.template = _.template($("#appdetails_template").html());

		this.model.bind('change', this.render, this);
    	this.model.fetch();
	},
	events: {
		"click .detailsRefreshAction":  "refresh"
	},
	//
	render: function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
    	return this;
	},
	refresh: function() {
		this.model.fetch();
		return false;
	}
});


var AppEnginelistModel = Backbone.Model.extend({
	url: function() {
		return getAPIUrl('apps/' + this.id + '/engines');
	}
});
var AppEnginelistView = Backbone.View.extend({
	/*
	 * Required Param: id  (app id)
	 */
	initialize: function(){
		this.id = this.options.id; // app id
		this.model = new AppEnginelistModel({"id": this.id});
		this.template = _.template($("#appenginelist_template").html());

		this.model.bind('change', this.render, this);
    	this.model.fetch();
	},
	render: function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
    	return this;
	},
});

var EngineStatusView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#engineStatus_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.model = new EngineModel({appid: this.appid, id: this.engineid});
	},
	events: {
		"click #engineStatusReloadBtn" : "render"
	},
	render : function() {
		this.poll(this);
		return this;
	},
	poll: function(view) {
		view.model.fetch({
			success: function() {
				view.$el.html(view.template({"data": view.model.toJSON()}));
				setTimeout(function() {view.poll(view);}, 2000);
			}
		});
		return view;
	}
});

var EngineView = Backbone.View.extend({
	initialize : function() {
		this.subViews = [];
		this.template_el = '#engine_template';
		this.template = _.template($(this.template_el).html());
		// TODO: Error checking for IDs
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.tabSettingsView == null;
		this.tabAlgorithmsView == null;
		this.tabExtraTabView == null;

    	this.breadcrumbView = new BreadcrumbView();
    	this.engineStatusView = new EngineStatusView();
    	this.subViews.push(this.breadcrumbView);
    	this.subViews.push(this.engineStatusView);
	},
	events: {
		"click #deleteEngineBtn":  "deleteEngine"
	},
	render : function() {
		this.$el.html(this.template());
		this.$el.find('#breadcrumb_ContentHolder').html(this.breadcrumbView.render().el);
		this.$el.find('#engineStatus_ContentHolder').html(this.engineStatusView.render().el);
		return this;
	},
	isEngineView : function() {
		return true;
	},
	closeExtraTab : function() {
		if (this.tabExtraTabView) {
			// remove data split slider for EngineSimEvalSetting page
			if (this.tabExtraTabView.isEngineSimEvalSettingsView) {
				$("#dataSplitBar").colResizable({disable:true});		
			}
			this.tabExtraTabView.close();
			this.$el.find('#engineExtraTabTitle').hide();
			this.$el.find("#engineExtraTabContentHolder").html("");
		}
		
	},
	showTabSettings : function() {
		this.closeExtraTab();
		this.$el.find("#engineTabSettingsBtn").tab('show');
       	if (!this.tabSettingsView) {
	        $.ajaxSetup({async:false}); // make jquery sync temporary to ensure it's loaded before we move on
	        this.$el.find("#engineTabSettingsContentHolder").load("enginebase/Engines/" + this.engineinfoid + "/engine.template.html");
	       	$.getScript("enginebase/Engines/" + this.engineinfoid + "/engine.js");
	       	$.ajaxSetup({async:true});
	       	// engine template and js should be ready by now
	       	this.tabSettingsView = createEngineView(this.appid, this.engineid);
	       	this.subViews.push(this.tabSettingsView);
	    } else {
	    	this.tabSettingsView.reloadData();
	    }
       	return false;
	},
	showTabAlgorithms : function() {
		this.closeExtraTab();
		this.$el.find("#engineTabAlgorithmsBtn").tab('show');

		if (!this.tabAlgorithmsView) {
			this.tabAlgorithmsView = new EngineAlgorithmsView();
			this.engineStatusView.listenTo(this.tabAlgorithmsView, 'engineStatusUpdate', this.engineStatusView.render); // listen to engineStatusUpdate event
			this.subViews.push(this.tabAlgorithmsView);
			this.$el.find("#engineTabAlgorithmsContentHolder").html(this.tabAlgorithmsView.render().el);
		} else {
			this.tabAlgorithmsView.reloadData();
		}
		return false;
	},
	showExtraTabAddAlgorithm : function() {
		this.closeExtraTab();
		this.$el.find('#engineExtraTabBtn').html("Add an Algorithm").tab('show');
		this.$el.find('#engineExtraTabTitle').show();

		this.tabExtraTabView = new EngineAddAlgorithmView();
		this.subViews.push(this.tabExtraTabView);
		this.$el.find("#engineExtraTabContentHolder").html(this.tabExtraTabView.render().el);
	},
	showExtraTabAlgorithmSettings : function(algoinfoid, algoName, algoid) {
		this.closeExtraTab();
		this.$el.find('#engineExtraTabBtn').html("Algorithm Settings: "+algoName).tab('show');
		this.$el.find('#engineExtraTabTitle').show();

        $.ajaxSetup({async:false}); // make jquery sync temporary to ensure it's loaded before we move on
        this.$el.find("#engineExtraTabContentHolder").load("enginebase/Engines/" + this.engineinfoid + "/Algorithms/" + algoinfoid + "/algorithm.template.html");
       	$.getScript("enginebase/Engines/" + this.engineinfoid + "/Algorithms/" + algoinfoid + "/algorithm.js");
       	$.ajaxSetup({async:true});
       	// algorithm template and js should be ready by now
       	this.tabExtraTabView = createAlgorithmView(this.appid, this.engineid, algoid, algoinfoid);
       	this.subViews.push(this.tabExtraTabView);
	},
	showExtraTabAlgoAutotuningReport : function(algoid) {
		this.closeExtraTab();
		this.$el.find('#engineExtraTabBtn').html("Algo Auto-tuning Report").tab('show');
		this.$el.find('#engineExtraTabTitle').show();

		this.tabExtraTabView = new EngineAlgoAutotuningReportView({id: algoid});
		this.subViews.push(this.tabExtraTabView);
		this.$el.find("#engineExtraTabContentHolder").html(this.tabExtraTabView.render().el);
	},
	showExtraTabSimEvalReport : function(simeval_id) {
		this.closeExtraTab();
		this.$el.find('#engineExtraTabBtn').html("Simulated Evaluation Report").tab('show');
		this.$el.find('#engineExtraTabTitle').show();

		this.tabExtraTabView = new EngineSimEvalReportView({id: simeval_id});
		this.subViews.push(this.tabExtraTabView);
		this.$el.find("#engineExtraTabContentHolder").html(this.tabExtraTabView.render().el);
	},
	showExtraTabSimEvalSettings : function(algoidlist) {
		this.closeExtraTab();
		this.$el.find('#engineExtraTabBtn').html("Simulated Evaluation Settings").tab('show');
		this.$el.find('#engineExtraTabTitle').show();

		this.tabExtraTabView = new EngineSimEvalSettingsView({algoidlist: algoidlist});
		this.subViews.push(this.tabExtraTabView);

		this.$el.find("#engineExtraTabContentHolder").html(this.tabExtraTabView.render().el).promise().done(function(){
			$("#dataSplitBar").colResizable({
				liveDrag: true,
				draggingClass: "rangeDrag",
				gripInnerHtml: "<div class='rangeGrip'></div>",
				onDrag: onSimEvalDataSplitPercentChange,
				minWidth: 12
			});
		});
	},
	deleteEngine : function() {
		var self = this;
		createDialog('Remove Engine?','This engine and all its data will be permanently deleted and cannot be recovered. Are you sure?', {
		      resizable: false,
		      height:185,
		      modal: true,
		      buttons: {
		        "Delete Engine": function() {
					var engineModel = new EngineModel({appid: self.appid, id: self.engineid});
					var erasingInfo = notifyInfoSticky('Removing engine. Please wait...','');
					engineModel.destroy({
						success: function() {
							notifyClear(erasingInfo);
							notifyInfoDefault('Engine removed.','');
							window.location = '/';
						},
		    			error: function(model, res) {
		    				notifyClear(erasingInfo);
		    				notifyErrorResponse(res);
		    			}
					});
		    		$( this ).dialog( "close" );
		        },
		        Cancel: function() {
		        	$( this ).dialog( "close" );
		        }
		      }
		});
		return false;
	}
});

/* Data Split Slider onChange in EngineSimEvalSettings */
var onSimEvalDataSplitPercentChange = function(e){
	var columns = $(e.currentTarget).find("td");
	var ranges = [], total = 0, i, w;
	for(i = 0; i<columns.length; i++) {
		// workaround, fix it later
		if (i==0) {
			w = columns.eq(i).width() - 9;
		} else if (i==2) {
			w = columns.eq(i).width() - 12;
		} else {
			w = columns.eq(i).width();
		}
		ranges.push(w);
		total+=w;
	}		 
	for(i=0; i<columns.length; i++){
		ranges[i] = 100*ranges[i]/total;
	}
	var trainPercent = Math.round(ranges[0]);
	var testPercent = Math.round(ranges[1]);
	var unusedPercent = 100 - trainPercent - testPercent;
	$('#simEvalSettingsForm').find('#splittrain').val(trainPercent).end().find('#splittest').val(testPercent).end();
};
/*
var onDataSplitPercentChange = function(e){
	var columns = $(e.currentTarget).find("td");
	var ranges = [], total = 0, i, w;
	for(i = 0; i<columns.length; i++) {
		// workaround, fix it later
		if (i==0) {
			w = columns.eq(i).width() - 35;
		} else if (i==3) {
			w = columns.eq(i).width() - 14;
		} else {
			w = columns.eq(i).width() -7;
		}
		ranges.push(w);
		total+=w;
	}		 
	for(i=0; i<columns.length; i++){
		ranges[i] = 100*ranges[i]/total;
	}
	var trainPercent = Math.round(ranges[0]);
	var validationPercent = Math.round(ranges[1]);
	var unusedPercent = Math.round(ranges[3]);
	var testPercent = 100 - trainPercent - validationPercent - unusedPercent;
	$('#simEvalSettingsForm').find('#splittrain').val(trainPercent).end().find('#splitvalidation').val(validationPercent).end().find('#splittest').val(testPercent).end();
};
*/


var EngineTypeListModel = Backbone.Model.extend({
	urlRoot: getAPIUrl('engineinfos')
});
var EngineModel = Backbone.Model.extend({
	/* Required param: appid */
	urlRoot: function(){
		return getAPIUrl("apps/" +this.get("appid") + "/engines");
	}
});
var AddEngineView = Backbone.View.extend({
	initialize: function(){
		this.subViews = [];
		this.template_el = '#app_addEngine_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");

		this.model = new EngineTypeListModel();
		this.model.bind('change', this.render, this);
    	this.model.fetch();

    	this.breadcrumbView = new BreadcrumbView();
    	this.subViews.push(this.breadcrumbView);
	},
	events: {
		"submit":  "createEngine"
	},
	createEngine: function(e) {
		$(e.target).find(".createEngineError").slideUp("fast").html(""); // clear error msg
		var engineData = formToJSON($(e.target)); // convert targeted form fields' names/values into key/value pairs
		engineData.appid = this.appid;
		var engineModel = new EngineModel();
		engineModel.save(engineData, {
	        success: function(model, resData) { // success, go to engine settings
	        	window.location = '?appid=' + resData.appid + '&engineid=' + resData.id + '&engineinfoid=' + resData.engineinfoid + '#engine';
	        },
	        error: function(model, res) {
	        	try { // show error message if fail
	        		var resData = $.parseJSON(res.responseText);
	        		$(e.target).find(".createEngineError").html(resData.message).slideDown("fast");
	        	} catch(err) {
	        		alert("An error has occured. HTTP Status Code: " + res.status);
	        	}
	        }

		});
		return false;
	},
	render: function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		this.$el.find('#breadcrumb_ContentHolder').html(this.breadcrumbView.render().el);
    	return this;
	},
});


var EngineAlgorithmsView = Backbone.View.extend({
	initialize : function() {
		this.subViews = [];
		this.template_el = '#engineTabAlgorithms_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.availableAlgoListView = new EngineAlgorithmsAvailableAlgoListView();
		this.listenTo(this.availableAlgoListView, 'changeSelected', this.changeAvailableAlgoSelected);
		this.subViews.push(this.availableAlgoListView);

		this.deployedAlgoView = new EngineAlgorithmsDeployedAlgoView();
		this.subViews.push(this.deployedAlgoView);

		this.simevalListView = new EngineAlgorithmsSimEvalListView();
		this.subViews.push(this.simevalListView);

		// Listen to deployDone, undeployDone events for refreshing display
		this.listenTo(this.deployedAlgoView, 'undeployDone', this.engineStatusUpdate); // listen to undeploy, trigger engineStatusUpdate when it happens
		this.listenTo(this.availableAlgoListView, 'deployDone', this.engineStatusUpdate); // listen to deploy, trigger engineStatusUpdate when it happens
		this.availableAlgoListView.listenTo(this.deployedAlgoView, 'undeployDone', this.availableAlgoListView.undeployDone);
		this.deployedAlgoView.listenTo(this.availableAlgoListView, 'deployDone', this.deployedAlgoView.deployDone);
	},
	events: {
		"click #availableAlgoDeployBtn":  "availableAlgoDeploy",
		"click #availableSimEvalBtn":  "availableAlgoSimEval"
	},
	render : function() {
		//this.$el.html(this.template({"data": this.model.toJSON()}));
		this.$el.html(this.template());
		this.$el.find('#engine_availableAlgoList_ContentHolder').html(this.availableAlgoListView.el);
		this.$el.find('#engine_deployedAlgo_ContentHolder').html(this.deployedAlgoView.render().el);
		this.$el.find('#engine_simevalList_ContentHolder').html(this.simevalListView.el);
		return this;
	},
	reloadData : function() {
		this.availableAlgoListView.reloadData();
		this.simevalListView.reloadData();
		this.deployedAlgoView.reloadData();
	},
	engineStatusUpdate: function() {
		this.trigger('engineStatusUpdate');
	},
	changeAvailableAlgoSelected: function(selectedAlgos) {
		if ($.isEmptyObject(selectedAlgos)) {
			this.$el.find('#availableAlgoDeployBtn').attr('disabled', true);
			this.$el.find('#availableSimEvalBtn').attr('disabled', true);
		} else {
			this.$el.find('#availableAlgoDeployBtn').removeAttr('disabled');
			this.$el.find('#availableSimEvalBtn').removeAttr('disabled');
		}
	},
	availableAlgoDeploy : function() {
		if (this.availableAlgoListView.hasSelectedAnyAlgo()){
			alert("Deploying multiple algorithms is not supported yet.");
			//this.availableAlgoListView.deploySelectedAlgos();  // uncomment this when deploy multi is ready
		}
		return false;
	},
	availableAlgoSimEval : function() {
		if (this.availableAlgoListView.hasSelectedAnyAlgo()){
			window.location.hash = 'simEvalSettings/'+ encodeURIComponentMapKey(this.availableAlgoListView.selectedAlgos);
		}
		return false;
	}
});

/* Required param: appid, engineid */
var DeployedAlgoModel = Backbone.Model.extend({
	urlRoot: function() {
		var path ='apps/' + this.get('appid') + '/engines/' + this.get('engineid') +'/algos_deployed';
		return getAPIUrl(path);
	}
});
var EngineAlgorithmsDeployedAlgoView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#engine_deployedAlgo_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid")
		this.model = new DeployedAlgoModel({appid: this.appid, engineid: this.engineid});
		this.model.bind('change', this.render, this);
    	this.model.fetch();
	},
	events: {
		'click #algoUndeployBtn': 'undeploy',
		'click #algoTrainNowBtn': 'trainnow',
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		return this;
	},
	undeploy: function() {
    	var self = this;
    	var path ='apps/' + this.appid + '/engines/' + this.engineid +'/algos_undeploy';
    	$.post(getAPIUrl(path), function() {
    		var appid = self.model.get('appid');
    		var engineid = self.model.get('engineid');
    		self.model.clear(); // empty display
    		self.model.set({appid: appid, engineid: engineid}); // put these back
    		self.trigger('undeployDone');
    	}).error(function(res) {
    		alert("An error has occured:" + res.status);
    	});
    	return false;
	},
	trainnow: function() {
    	var self = this;
    	var path ='apps/' + this.appid + '/engines/' + this.engineid +'/algos_trainnow';
        var info = notifyInfo('Requesting to train data model immediately...', '', {positionClass: 'toast-bottom-right', fadeOut: 1});
    	$.post(getAPIUrl(path), function(res) {
            notifyClear(info);
            notifySuccess(res.message, '', {positionClass: 'toast-bottom-right', timeOut: 2800});
    	}).error(function(res) {
        	try { // show error message if fail
        		var resData = $.parseJSON(res.responseText);
	            notifyClear(info);
	            notifyErrorSticky("HTTP " + res.status + ": " + resData.message, '');
        	} catch(err) {
        		alert("An error has occured. HTTP Status Code: " + res.status);
        	}
    	});
	},
	deployDone: function() {
		this.model.fetch();
	},
	reloadData: function() {
		this.model.fetch();
	}
});

/* Required param: appid, engineid */
var AvailableAlgoModel = Backbone.Model.extend({
	urlRoot: function() {
		var path ='apps/' + this.get('appid') + '/engines/' + this.get('engineid') +'/algos_available';
		return getAPIUrl(path);
	}
});
/* Required param: appid, engineid */
var AvailableAlgoListCollection = Backbone.Collection.extend({
	model: AvailableAlgoModel,
	initialize: function(models, options) {
		this.url = getAPIUrl('apps/' + options.appid + '/engines/' + options.engineid +'/algos_available');
	}
});
var EngineAlgorithmsAvailableAlgoListView = Backbone.View.extend({
	initialize : function() {
		this.subViews = [];
		this.selectedAlgos = {};
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");

		this.template_el = '#engine_availableAlgoList_template';
		this.template = _.template($(this.template_el).html());
		this.$el.html(this.template());

    	this.collection = new AvailableAlgoListCollection([], {appid: this.appid, engineid: this.engineid});
    	this.collection.bind('add', this.addOne, this);
    	this.collection.bind('reset', this.render, this);
    	this.collection.fetch();
	},
	render: function(eventData) {
		this.$el.find('tbody').html(""); // clear previous items in table
		_.each(eventData.models, function(model){
			this.addOne(model); // debug: console.log(app.attributes['id']);
		}, this);
		return this;
	},
    addOne: function(model) {
        var avaAlgoView = new EngineAlgorithmsAvailableAlgoView({ model:model});
        this.listenTo(avaAlgoView, 'select', this.algoSelect);
        this.listenTo(avaAlgoView, 'unselect', this.algoUnselect);
        this.listenTo(avaAlgoView, 'deploy', this.deploySingleAlgo);
        this.subViews.push(avaAlgoView);
        this.$el.find('tbody').append( avaAlgoView.render().el );
    },
    algoSelect: function(algoid) {
    	this.selectedAlgos[algoid] = true;
    	this.trigger('changeSelected', this.selectedAlgos);
    },
    algoUnselect: function(algoid) {
    	if (this.selectedAlgos[algoid]) {
    		delete this.selectedAlgos[algoid];
    		this.trigger('changeSelected', this.selectedAlgos);
    	}
    },
    deploySingleAlgo: function(algoid) {
    	this.deploy([algoid]);
    },
    deploySelectedAlgos: function() {
    	if(!$.isEmptyObject(this.selectedAlgos)) { // if selectedAlgos is not empty
    		this.deploy(MapKeyToArray(this.selectedAlgos));
    	}
    },
    deploy: function(algoidlist) { // common func for deploying single/multiple algo(s)
    	var self = this;
    	var path ='apps/' + this.appid + '/engines/' + this.engineid +'/algos_deploy';
    	$.ajax({
    		type: "POST",
    		url: getAPIUrl(path),
    		data: JSON.stringify({algoidlist: algoidlist}),
    		contentType: "application/json; charset=utf-8",
    		success: function() {
	    		self.collection.fetch();
	    		self.trigger('deployDone');
	    	}
    	}).error(function(res) {
    		alert("An error has occured:" + res.status);
    	});
    },
	undeployDone: function() {
		this.collection.fetch();
	},
	hasSelectedAnyAlgo: function() { // determine if user has selected the checkbox of any available algo
		if ($.isEmptyObject(this.selectedAlgos)) {
			return false;
		} else {
			return true;
		}
	},
	reloadData: function(){
		this.selectedAlgos = {}
		this.trigger('changeSelected', this.selectedAlgos);
		this.collection.fetch();
	}
});
/* required: model */
var EngineAlgorithmsAvailableAlgoView = Backbone.View.extend({
	tagName: 'tr',
	initialize : function() {
		this.template_el = '#engine_availableAlgo_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		//this.model.bind('change', this.render, this);
    	//this.model.fetch();
	},
	events: {
		//'':'customFunction'
		"change input[type='checkbox']":  "selectToggle",
		"click .algoDeployBtn": "deploy",
		"click .algoDeleteBtn": "delete"
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		return this;
	},
	selectToggle: function(e) {
		if (e.currentTarget.checked == true) {
			this.trigger('select', this.model.get('id'));
		} else {
			this.trigger('unselect', this.model.get('id'));
		}
	},
	deploy: function(e) {
		this.trigger('deploy', this.model.get('id'));
		return false;
	},
	delete: function() {
		var self = this;
		var erasingInfo = notifyInfoSticky('Removing algorithm. Please wait...','');
		this.model.destroy({
			success: function(model, res) {
				notifyClear(erasingInfo);
				notifyInfoDefault('Algorithm removed.','');
				self.close();
			},
			error: function(model, res) {
				notifyClear(erasingInfo);
				notifyErrorResponse(res);
			},
		});
		return false;
	}
});

/* Required param: appid, engineid */
var SimEvalModel = Backbone.Model.extend({
	urlRoot: function() {
		var path ='apps/' + this.get('appid') + '/engines/' + this.get('engineid') +'/simevals';
		return getAPIUrl(path);
	}
});
/* Required param: appid, engineid */
var SimEvalListCollection = Backbone.Collection.extend({
	model: SimEvalModel,
	initialize: function(models, options) {
		this.url = getAPIUrl('apps/' + options.appid + '/engines/' + options.engineid +'/simevals');
	}
});
var EngineAlgorithmsSimEvalListView = Backbone.View.extend({
	initialize : function() {
		this.subViews = [];
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");

		this.template_el = '#engine_simevalList_template';
		this.template = _.template($(this.template_el).html());
		this.$el.html(this.template());

    	this.collection = new SimEvalListCollection([], {appid: this.appid, engineid: this.engineid});
    	this.collection.bind('add', this.addOne, this);
    	this.collection.bind('reset', this.render, this);
    	this.collection.fetch();
	},
	render: function(eventData) {
		this.$el.find('tbody').html(""); // clear previous items in table
		_.each(eventData.models, function(model){
			this.addOne(model); // debug: console.log(app.attributes['id']);
		}, this);
		return this;
	},
    addOne: function(model) {
        var simevalView = new EngineAlgorithmsSimEvalView({ model:model});
        this.subViews.push(simevalView);
        this.$el.find('tbody').append( simevalView.render().el );
    },
    reloadData: function(){
    	this.collection.fetch();
    }
});
/* required: model */
var EngineAlgorithmsSimEvalView = Backbone.View.extend({
	tagName: 'tr',
	initialize : function() {
		this.template_el = '#engine_simeval_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		//this.model.bind('change', this.render, this);
    	//this.model.fetch();
	},
	events: {
		"click .simevalDeleteBtn":  "delete",
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		return this;
	},
	delete: function() {
		var self = this;
		var erasingInfo = notifyInfoSticky('Removing simulated evaluation. Please wait...','');
		this.model.destroy({
			success: function(model, res) {
				notifyClear(erasingInfo);
				notifyInfoDefault('Simulated evaluation removed.','');
				self.close();
			},
			error: function(model, res) {
				notifyClear(erasingInfo);
				notifyErrorResponse(res);
			}
		});
	}
});


var EnginetypeAlgorithmListModel = Backbone.Model.extend({
	url: function() {
		return getAPIUrl('engineinfos/' + this.id + '/algoinfos');
	}
});
var EngineAddAlgorithmView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#engine_addAlgorithm_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.model = new EnginetypeAlgorithmListModel({"id": this.engineinfoid});
		this.model.bind('change', this.render, this);
    	this.model.fetch();
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		return this;
	},
	events: {
		"submit":  "addAlgorithm"
	},
	addAlgorithm: function(e) {
		$(e.target).find(".addAlgoError").slideUp("fast").html(""); // clear error msg
		var algoData = formToJSON($(e.target)); // convert targeted form fields' names/values into key/value pairs
		algoData.appid = this.appid;
		algoData.engineid = this.engineid;
		var algoModel = new AvailableAlgoModel();
		algoModel.save(algoData, {
	        success: function(model, resData) { // success, go to algo settings
	        	window.location.hash = 'algoSettings/' + decodeURIComponent(resData.algoinfoid) + '/'+ decodeURIComponent(resData.algoname) + '/' + decodeURIComponent(resData.id);
	        },
	        error: function(model, res) {
	        	try { // show error message if fail
	        		var resData = $.parseJSON(res.responseText);
	        		$(e.target).find(".addAlgoError").html(resData.message).slideDown("fast");
	        	} catch(err) {
	        		alert("An error has occured. HTTP Status Code: " + res.status);
	        	}
	        }
		});
		return false;
	}
});

var EngineAlgoAutotuningReportModel = Backbone.Model.extend({
	initialize: function(model, options) {
		//this.urlRoot = getAPIUrl('apps/' + options.appid + '/engines/' + options.engineid +'/algoautotuning_report'); # TODO: remove
		this.url = getAPIUrl('apps/' + options.appid + '/engines/' + options.engineid +'/algos_available/' + this.id + '/autotune_report');
	}
});
var EngineAlgoAutotuningReportView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#engine_algoAutotuningReport_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.algoid = this.options.id;		
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.model = new EngineAlgoAutotuningReportModel({"id": this.algoid}, {appid: this.appid, engineid: this.engineid});
		this.model.bind('change', this.render, this);
    	this.model.fetch();
	},
	events: {
		"click .algoAutotuneSelectBtn":  "selectAutotune"
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		console.log(this.model.toJSON());
		return this;
	},
	selectAutotune: function(e) {
		var tunedalgoid = $(e.target).data('autotuneid');
    	var path ='apps/' + this.appid + '/engines/' + this.engineid +'/algos_available/' + this.algoid + '/autotune_apply';
    	$.ajax({
    		type: "POST",
    		url: getAPIUrl(path),
    		data: JSON.stringify({tunedalgoid: tunedalgoid}),
    		contentType: "application/json; charset=utf-8",
    		success: function() {
	    		window.location.hash = 'engineTabAlgorithms';
	    	}
    	}).error(function(res) {
    		alert("An error has occured:" + res.status);
    	});
		return false;
	}
});

var EngineSimEvalReportModel = Backbone.Model.extend({
	initialize: function(model, options) {
		//this.urlRoot = getAPIUrl('app/' + options.appid + '/engine/' + options.engineid +'/simeval_report');
		this.url = getAPIUrl('apps/' + options.appid + '/engines/' + options.engineid +'/simevals/' + this.id + '/report');
	}
});
/* Required Param: id  (simulated eval report id) */
var EngineSimEvalReportView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#engine_simEvalReport_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.simeval_id = this.options.id;
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.model = new EngineSimEvalReportModel({"id": this.simeval_id}, {appid: this.appid, engineid: this.engineid});
		this.model.bind('change', this.render, this);
    	this.model.fetch();
	},
	render : function() {
		this.$el.html(this.template({"data": this.model.toJSON()}));
		return this;
	}
});




var EnginetypeMetricsTypeListModel = Backbone.Model.extend({
	url: function() {
		return getAPIUrl('engineinfos/' + this.id + "/metricinfos");
	}
});
/* Required Param: algoidlist  (algo ids to be evaluated) */
var EngineSimEvalSettingsView = Backbone.View.extend({
	initialize : function() {
		this.isEngineSimEvalSettingsView = true;
		this.subViews = [];
		this.template_el = '#engine_simEvalSettings_template';
		this.template = _.template($(this.template_el).html()); // define template function
		this.simeval_id = this.options.id;
		this.algoidlist = this.options.algoidlist;
		this.form_el = '#simEvalSettingsForm';
		this.appid = getUrlParam("appid");
		this.engineid = getUrlParam("engineid");
		this.engineinfoid = getUrlParam("engineinfoid");
		this.indexCount = 0;
//		this.model = new EngineSimEvalSettingsModel({"id": this.simeval_id});
//		this.model.bind('change', this.render, this);
//    	this.model.fetch();
	},
	events: {
		'submit':  'save',
		'click #addMetricsBtn': 'addMetrics'
	},
	render : function() {
		$.ajaxSetup({async:false});  // ensure sync, necessary for Data Split Slider
		//this.$el.html(this.template({"data": this.model.toJSON()}));
		// construct algo id list
		var self = this;

		// construct algo name list (this.algoName_list) from inputted algo id list
		var algoArrayRaw = this.algoidlist.split(',');
		this.algoList = algoArrayRaw.map(function(algoid_encoded){
			var algoid = decodeURIComponent(algoid_encoded);
			var algoModel = new AvailableAlgoModel({appid: self.appid, engineid: self.engineid, id: algoid});
			algoModel.fetch();
			return {algoname: algoModel.get('algoname'), id: algoModel.get('id')};
		});

		// render metrics options
		metricstypeListModel = new EnginetypeMetricsTypeListModel({id: this.engineinfoid});
		metricstypeListModel.fetch({
				success: function(model, res) {
					self.metricslist = res.metricslist;
					self.$el.html(self.template({data: { algoList: self.algoList }}));
					self.addOne();
				}
		});
		$.ajaxSetup({async:true}); // end of ensure sync, necessary for Data Split Slider
		return this;
	},
	addMetrics: function() {
		this.addOne();
		return false;
	},
	addOne: function() {
		var metricsView = new EngineSimEvalSettingsMetricsView({
			data: {
				index: this.indexCount,
				metricslist: this.metricslist
			}
		});
		this.indexCount += 1; // increase count
		this.subViews.push(metricsView);
		this.$el.find('#metrics_list_ContentHolder').append(metricsView.render().el);
	},
	save: function() {
		$(this.error_el).slideUp().html(""); // reset/clear all error msg
		var data = formToJSON(this.$el.find(this.form_el)); // convert form names/values of fields into key/value pairs
		var simEvalModel = new SimEvalModel({appid: this.appid, engineid: this.engineid});
		var self = this;
		simEvalModel.save(data, {
			wait: true,
			success: function(model, res) {
				window.location.hash = 'engineTabAlgorithms';
			},
			error: function(model, res){
				notifyErrorResponse(res);
				/*
				alert("An error has occured. HTTP Status Code: "
						+ res.status);*/
			}
		});
		return false;
	}
});

var EngineSimEvalSettingsMetricsView = Backbone.View.extend({
	initialize : function() {
		this.template_el = '#engine_simEvalSettingsMetrics_template';
		this.template = _.template($(this.template_el).html());
		this.data = this.options.data;
	},
	events: {
		'click .deleteMetricsBtn': 'delete'
	},
	render : function() {
		//this.$el.html(this.template({"data": this.model.toJSON()}));
		this.$el.html(this.template({data: this.data}));
		return this;
	},
	delete: function() {
		this.remove();
		this.close();
		return false;
	}
});

