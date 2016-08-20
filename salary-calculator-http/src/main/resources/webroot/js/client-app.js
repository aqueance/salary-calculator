/*
 * Copyright (c) 2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

var transformations = {
    'hidden': 'hidden',
    'error': 'text-danger',
    'container': 'container',
    'row': 'row',
    'page-title': 'col-xs-12',
    'file-upload-view': 'col-xs-4 col-sm-3 col-md-2',
    'salary-list-view': 'col-xs-8 col-sm-9 col-md-10',
    'upload-active': 'upload-icon fa fa-cog fa-spin fa-fw text-danger',
    'upload-ready': 'upload-icon fa fa-upload active',
    'pretty-table': 'table table-striped table-hover table-condensed',
    'file-name': 'text-info',
    'month': 'pull-right'
};

var transform = 'transform';

function transformer(node) {
    if (node && typeof node === 'object') {
        var attributes = node.attrs;

        if (attributes && typeof attributes[transform] === 'string') {
            var transformed = transformations[attributes[transform]];

            if (transformed) {
                delete attributes[transform];

                var className = attributes.className;
                attributes.className = !className ? transformed : (className + ' ' + transformed);
            }
        }

        var children = node.children;

        if (children && typeof children.forEach === 'function') {
            children.forEach(transformer);
        }
    }

    return node;
}

function marker(tag) {
    var marks = Array.apply(null, arguments).slice(1);
    return tag + '[' + transform + '=' + marks.join('-') + ']';
}

(function(element, tag, transform) {

    // See http://lhorie.github.io/mithril-blog/drag-n-drop-file-uploads.html
    var FileUpload = (function() {
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

        FileUpload.property = Object.create(null);

        return FileUpload;
    })();

    // Mithril view model.
    var Model = (function() {
        function Model() {
            var model = this;

            // bind these as instance properties
            ['controller', 'view'].forEach(function(property) {
                var value = model[property];

                if (typeof value === 'function') {
                    Object.defineProperty(model, property, { value: value.bind(model) });
                }
            });
        }

        Model.prototype = Object.create(null);

        return Model;
    })();

    // This component displays errors
    var ErrorModel = (function(Model) {
        function ErrorModel() {
            Model.apply(this);

            this.data = m.prop('');
        }

        ErrorModel.prototype = Object.create(Model.prototype, {
            view: {
                value: function() {
                    return transform(m(tag('div', 'error'), m('strong', this.data())));
                }
            }
        });

        return ErrorModel;
    })(Model);

    // This component shows a list of monthly salaries
    var SalaryListModel = (function(Model) {
        function SalaryListModel() {
            Model.apply(this);

            this.name = m.prop('');     // the name of the uploaded file
            this.data = m.prop();       // the data returned by the server

            var self = this;

            // notification that a new file upload is taking place
            this.loading = function(name) {
                self.name(name);
                self.data(null);
            };
        }

        SalaryListModel.prototype = Object.create(Model.prototype, {
            view: {
                value: function() {
                    var data = this.data();

                    if (!data) return m(tag('div', 'hidden'));
                    if (data.error) return m(tag('div', 'error'), data.error);

                    var name = this.name();
                    var view = m(tag('table', 'pretty-table'),
                        Array.prototype.concat.apply([], data.months.map(function(subject) {
                            return [
                                m('thead', [
                                    m('tr', [
                                        m(tag('th[colspan=3]', 'file-name'), [
                                            m(tag('div', 'month'), subject.month + '/' + subject.year),
                                            name
                                        ])
                                    ]),
                                    m('tr', [
                                        m('th', 'ID'),
                                        m('th', 'Employee'),
                                        m('th', 'Salary')
                                    ])
                                ]),
                                m('tbody', subject.people.map(function(employee) {
                                    return m('tr', [
                                        m('td', employee.id),
                                        m('td', employee.name),
                                        m('td', employee.salary)
                                    ]);
                                }))
                            ];
                        }))
                    );

                    return transform(view);
                }
            }
        });

        return SalaryListModel;
    })(Model);

    // This component shows the file upload receptor and spinner
    var FileUploadModel = (function(Model, FileUpload) {
        function FileUploadModel() {
            Model.apply(this);

            this.loading = m.prop(false);
        }

        FileUploadModel.prototype = Object.create(Model.prototype, {
            controller: {
                value: function(options) {
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
                }
            },
            view: {
                value: function(initialize) {
                    var view = m(tag('div', 'upload', this.loading() ? 'active' : 'ready'), {
                        config: function(element, initialized) {
                            if (!initialized) {
                                initialize(element);
                            }
                        }
                    });

                    return transform(view);
                }
            }
        });

        return FileUploadModel;
    })(Model, FileUpload);

    // This component shows the header
    var HeadingModel = (function(Model) {
        function HeadingModel(text) {
            Model.apply(this);

            this.text = m.prop(text);
        }

        HeadingModel.prototype = Object.create(Model.prototype, {
            view: {
                value: function() {
                    return transform(m(tag('h3', 'page-title'), this.text()));
                }
            }
        });

        return HeadingModel;
    })(Model);

    var SalaryCalculatorPage = (function(Model) {
        function SalaryCalculatorPage(url) {
            Model.apply(this);

            this.headingModel = new HeadingModel('Drop an hour list file onto the box:');
            this.errorModel = new ErrorModel();
            this.fileUploadModel = new FileUploadModel();
            this.salaryListModel = new SalaryListModel();

            this.url = url;
        }

        SalaryCalculatorPage.prototype = Object.create(Model.prototype, {
            view: {
                value: function() {
                    var view = m(tag('div', 'container'), [
                        m(tag('div', 'row'), m(this.headingModel)),
                        m(tag('div', 'row'), [
                            m(tag('div', 'file-upload-view'), [
                                m(this.fileUploadModel, {
                                    url: this.url,
                                    loading: this.salaryListModel.loading,
                                    success: this.salaryListModel.data,
                                    error: this.errorModel.data
                                }),
                                m(this.errorModel)
                            ]),
                            m(tag('div', 'salary-list-view'), m(this.salaryListModel))
                        ])
                    ]);

                    return transform(view);
                }
            }
        });

        return SalaryCalculatorPage;
    })(Model);

    m.mount(element, m(new SalaryCalculatorPage('calculate')));
})(document.body, marker, transformer);
