/* global ko */
MyCustomerViewModel = function() {
    var self = this;
    self.items = ko.observableArray();
    $.getJSON("http://localhost:47027/BowerPOM/webresources/customers").
            then(function(customers) {
                $.each(customers, function() {
                    self.items.push({
                        city: ko.observable(this.city),
                        phone: ko.observable(this.phone),
                        name: ko.observable(this.name),
                        addressline2: ko.observable(this.addressline2),
                        creditLimit: ko.observable(this.creditLimit),
                        addressline1: ko.observable(this.addressline1),
                        state: ko.observable(this.state),
                        fax: ko.observable(this.fax),
                        email: ko.observable(this.email)
                    });
                });
            }); 
};
ko.applyBindings(new MyCustomerViewModel());