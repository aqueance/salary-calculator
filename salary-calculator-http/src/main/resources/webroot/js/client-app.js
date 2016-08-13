'use strict';

(function(element) {
    function ErrorModel() {
        this.data = m.prop('');

        this.view = ErrorModel.view.bind(this);
    }

    ErrorModel.view = function() {
        var data = this.data();
        return m('div' + (!data ? '.hidden' : '.text-danger'), m('strong', data));
    };

    function SalaryListModel() {
        this.name = m.prop('');
        this.data = m.prop();

        var self = this;
        this.loading = function(name) {
            self.name(name);
            self.data(null);
        };

        this.view = SalaryListModel.view.bind(this);
    }

    SalaryListModel.view = function() {
        var data = this.data();

        if (!data) return m('div.hidden');
        if (data.error) return m('div.text-danger', data.error);

        var name = this.name();
        return m('table.table.table-striped.table-bordered.table-hover.table-condensed', [
            data.months.map(function(month) {
                return [
                    m('thead', [
                        m('tr', [
                            m('th[colspan=3].text-info', [
                                m('div.pull-right', month.month + '/' + month.year),
                                name
                            ])
                        ]),
                        m('tr', [
                            m('th', 'ID'),
                            m('th', 'Employee'),
                            m('th', 'Salary')
                        ])
                    ]),
                    m('tbody', month.people.map(function(employee) {
                        return m('tr', [
                            m('td', employee.id),
                            m('td', employee.name),
                            m('td', employee.salary)
                        ]);
                    }))
                ];
            })
        ]);
    };

    var FileUploadModel = (function() {

        // See http://lhorie.github.io/mithril-blog/drag-n-drop-file-uploads.html

        function FileUpload(options) {
            var url = !options ? undefined : options.url;

            if (!url) {
                throw new Error('Upload URL missing')
            }

            this.dragdrop = function(element, options) {
                options = options || {};

                var enabled = options.enabled || function() {
                    return true;
                };

                function activate(event) {
                    event.preventDefault();

                    var target = event.currentTarget;
                    if (enabled() && !~target.className.indexOf('accept-drag')) {
                        target.className = target.className.split(/\s+/).concat('accept-drag').join(' ');
                    }
                }

                function deactivate() {
                    var target = event.currentTarget;
                    target.className = target.className.split(/\s+/).filter(function(name) {
                        return name != 'accept-drag';
                    }).join(' ');
                }

                function update(event) {
                    event.preventDefault();

                    if (typeof options.onchange == 'function') {
                        options.onchange((event.dataTransfer || event.target).files[0])
                    }
                }

                element.addEventListener('dragover', activate);
                element.addEventListener('dragleave', deactivate);
                element.addEventListener('dragend', deactivate);
                element.addEventListener('drop', deactivate);
                element.addEventListener('drop', update);
            };

            this.upload = function(file) {
                var formData = new FormData;

                formData.append('file', file);

                return m.request({
                    method: 'POST',
                    url: url,
                    data: formData,
                    serialize: function(value) {
                        return value;
                    }
                });
            };
        }

        function FileUploadModel() {
            this.loading = m.prop(false);

            this.controller = FileUploadModel.controller.bind(this);
            this.view = FileUploadModel.view.bind(this);
        }

        FileUploadModel.controller = function(options) {
            if (!options || typeof options.loading !== 'function') throw new Error('No options.loading specified or it is not a function: ' + options.loading);
            if (!options || typeof options.success !== 'function') throw new Error('No options.success specified or it is not a function; ' + options.success);
            if (!options || typeof options.error !== 'function') throw new Error('No options.error specified or it is not a function: ' + options.error);

            var self = this;

            options.enabled = function() {
                return !self.loading();
            };

            var files = new FileUpload(options);

            return function(element) {
                files.dragdrop(element, {
                    onchange: function(file) {
                        if (!self.loading()) {
                            if (file.type !== 'text/csv') {
                                options.error('That was not a CSV file.');
                            } else {
                                self.loading(true);

                                options.error(null);
                                options.loading(file.name);

                                files.upload(file).then(function(data) {
                                    self.loading(false);
                                    options.success(data);
                                }, function(data) {
                                    self.loading(false);
                                    options.error(data || 'Communicate failure with the server.');
                                });
                            }

                            m.redraw();
                        }
                    }
                });
            };
        };

        FileUploadModel.view = function(initialize) {
            return m('div.upload-icon.fa' + (this.loading() ? '.fa-cog.fa-spin.fa-fw.text-danger' : '.fa-upload.active'), {
                config: function(element, initialized) {
                    if (!initialized) {
                        initialize(element);
                    }
                }
            });
        };

        return FileUploadModel;
    })();

    function UploadComposite(fileUploadModel, salaryListModel, errorModel) {
        this.fileUploadModel = fileUploadModel;
        this.errorModel = errorModel;
        this.salaryListModel = salaryListModel;

        this.view = UploadComposite.view.bind(this);
    }

    UploadComposite.view = function(controller, options) {
        return m('div' + (options.width || '.col-xs-12'), [
            m(this.fileUploadModel, {
                url: '/calculate',
                loading: this.salaryListModel.loading,
                success: this.salaryListModel.data,
                error: this.errorModel.data
            }),
            m(this.errorModel)
        ]);
    };

    function SalariesComposite(salaryListModel) {
        this.salaryListModel = salaryListModel;

        this.view = SalariesComposite.view.bind(this);
    }

    SalariesComposite.view = function(controller, options) {
        return m('div' + (options.width || '.col-xs-12'), m(this.salaryListModel));
    };

    function SalaryCalculatorPage(headingModel, uploadComposite, salariesComposite) {
        this.headingModel = headingModel;
        this.uploadComposite = uploadComposite;
        this.salariesComposite = salariesComposite;

        this.view = SalaryCalculatorPage.view.bind(this);
    }

    SalaryCalculatorPage.view = function() {
        return m('div.container', [
            m('div.row', this.headingModel),
            m('div.row', [
                m(this.uploadComposite, { width: '.col-xs-4.col-sm-3.col-md-2' }),
                m(this.salariesComposite, { width: '.col-xs-8.col-sm-9.col-md-10' })
            ])        ]);
    };

    function HeadingModel(text) {
        this.text = m.prop(text);

        this.view = HeadingModel.view.bind(this);
    }

    HeadingModel.view = function() {
        return m('h3.col-xs-12', this.text());
    };

    var errorModel = new ErrorModel();
    var salaryListModel = new SalaryListModel();
    var fileUploadModel = new FileUploadModel();

    var uploadComposite = new UploadComposite(fileUploadModel, salaryListModel, errorModel);
    var salariesComposite = new SalariesComposite(salaryListModel);

    var headingModel = new HeadingModel('Drop an hour list file onto the box:');
    var salaryCalculatorPage = new SalaryCalculatorPage(headingModel, uploadComposite, salariesComposite);

    m.mount(element, m(salaryCalculatorPage));
})(document.body);
