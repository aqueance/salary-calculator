'use strict';

(function(element) {
    function SalaryList(options) {
        this.fileName = m.prop('');
        this.content = m.prop({});
    }

    var ErrorComponent = {
        view: function(controller, options) {
            // TODO: error template
            return m('div.text-danger', m('strong', 'Upload error component'));
        }
    };

    var FileUploadComponent = (function() {

        // Returns a function that returns its single parameter after optionally calling another function with it.
        function unchanged(callback) {
            return function(data) {
                if (callback) callback(data);
                return data;
            };
        }

        // See http://lhorie.github.io/mithril-blog/drag-n-drop-file-uploads.html

        function FileUpload(options) {
            var url = !options ? undefined : options.url;

            if (!url) {
                throw new Error("Upload URL missing")
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

                    if (typeof options.onchange == "function") {
                        options.onchange((event.dataTransfer || event.target).files)
                    }
                }

                element.addEventListener("dragover", activate);
                element.addEventListener("dragleave", deactivate);
                element.addEventListener("dragend", deactivate);
                element.addEventListener("drop", deactivate);
                element.addEventListener("drop", update);
            };

            this.upload = function(files) {
                var formData = new FormData;

                for (var i = 0; i < files.length; i++) {
                    formData.append("file" + i, files[ i ])
                }

                return m.request({
                    method: "POST",
                    url: url,
                    data: formData,
                    serialize: unchanged()
                });
            };
        }

        return {
            controller: function(options) {
                var self = this;

                self.model = {
                    loading: false
                };

                options.enabled = function() {
                    return !self.loading;
                };

                var files = new FileUpload(options);

                this.initialize = function(element) {
                    files.dragdrop(element, {
                        onchange: function(data) {
                            var vm = self.model;

                            if (!vm.loading) {
                                vm.loading = true;

                                files.upload(data).then(unchanged(function() {
                                    vm.loading = false;
                                }));

                                m.redraw();
                            }
                        }
                    });
                }
            },
            view: function(controller) {
                return m('div.upload-icon.fa' + (controller.model.loading ? '.fa-refresh.fa-spin.fa-fw.text-danger' : '.fa-upload.active'), {
                    config: function(element, initialized) {
                        if (!initialized) {
                            controller.initialize(element);
                        }
                    }
                });
            }
        };
    })();

    var UploadComposite = {
        view: function(controller, options) {
            return m('div' + (options.width || '.col-xs-12'), [
                m(FileUploadComponent, { url: '/calculate' }),
                m(ErrorComponent)
            ]);
        }
    };

    var FileNameComponent = {
        view: function(controller, options) {
            // TODO: uploaded file name
            return m('div', m('strong', 'Uploaded file name component'));
        }
    };

    var SalaryListComponent = {
        view: function(controller, options) {
            // TODO: list template
            return m('div', 'Salary list component');
        }
    };

    var SalariesComposite = {
        view: function(controller, options) {
            return m('div' + (options.width || '.col-xs-12'), [
                m(FileNameComponent),
                m(SalaryListComponent)
            ]);
        }
    };

    var SalaryCalculatorPage = {
        controller: function(options) {
            // TODO
            return options;
        },
        view: function(controller, options) {
            return m('div.container',
                m('div.row', [
                    m(UploadComposite, { width: '.col-xs-4.col-sm-3.col-md-2' }),
                    m(SalariesComposite, { width: '.col-xs-8.col-sm-9.col-md-10' })
                ]));
        }
    };

    m.mount(element, m(SalaryCalculatorPage));
})(document.body);
