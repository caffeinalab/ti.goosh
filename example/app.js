// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.


// open a single window
var win = Ti.UI.createWindow({
	backgroundColor:'white'
});
var label = Ti.UI.createLabel();
win.add(label);
win.open();

// TODO: write your module tests here
var ti_goosh = require('ti.goosh');
Ti.API.info("module is => " + ti_goosh);

label.text = ti_goosh.example();

Ti.API.info("module exampleProp is => " + ti_goosh.exampleProp);
ti_goosh.exampleProp = "This is a test value";

if (Ti.Platform.name == "android") {
	var proxy = ti_goosh.createExample({
		message: "Creating an example Proxy",
		backgroundColor: "red",
		width: 100,
		height: 100,
		top: 100,
		left: 150
	});

	proxy.printMessage("Hello world!");
	proxy.message = "Hi world!.  It's me again.";
	proxy.printMessage("Hello world!");
	win.add(proxy);
}

