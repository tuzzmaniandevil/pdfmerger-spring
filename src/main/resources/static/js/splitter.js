(function ($) {
    // Init Reset Button
    function initReset() {
        $('body').on('click', '.btn-reset', function (e) {
            e.preventDefault();

            resetForm();
        });
    }

    function resetForm() {
        $('#pdfForm').trigger('reset');
    }

    function initForm() {
        var form = $('#pdfForm');

        form.on('submit', function (e) {
            e.preventDefault();
            var files = form.find('#file')[0].files;

            if (files.length <= 0) {
                return false;
            } else {
                disableForm(true);

                var fileToUpload = files[0];

                console.log(fileToUpload, fileToUpload, fileToUpload.name);

                var formData = new FormData();
                formData.append('file', fileToUpload, fileToUpload.name);

                var ajaxOpts = {
                    type: 'POST',
                    url: '/splitPdf',
                    data: formData,
                    dataType: 'binary',
                    processData: false,
                    contentType: false,
                    xhrFields: {
                        responseType: 'blob'
                    },
                    beforeSend: function (xhr, options) {
                        options.data = formData;

                        /**
                         * You can use https://github.com/francois2metz/html5-formdata for a fake FormData object
                         * Only work with Firefox 3.6
                         */
                        if (formData.fake) {
                            xhr.setRequestHeader('Content-Type', 'multipart/form-data; boundary=' + formData.boundary);
                            // with fake FormData object, we must use sendAsBinary
                            xhr.send = function (data) {
                                xhr.sendAsBinary(data.toString());
                            };
                        }
                    },
                    success: function (resp, status, xhr) {
                        var filename = "";
                        var disposition = xhr.getResponseHeader('Content-Disposition');
                        if (disposition && disposition.indexOf('attachment') !== -1) {
                            var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                            var matches = filenameRegex.exec(disposition);
                            if (matches != null && matches[1])
                                filename = matches[1].replace(/['"]/g, '');
                        }

                        if (filename === null || typeof filename === 'undefined' || filename.length < 1) {
                            filename = 'output.zip';
                        }
                        saveFile(resp, filename);
                        disableForm(false);
                        swal("Success!", "Your PDF has been split", "success");
                    },
                    error: function () {
                        swal("Oh No!", "Something went wrong!", "error");
                        disableForm(false);
                    }
                };

                $.ajax(ajaxOpts);
            }

            return false;
        });
    }

    function disableForm(disabled) {
        var form = $('#pdfForm');

        form.find("input").attr("disabled", disabled);
        form.find("button").attr("disabled", disabled);
    }

    function saveFile(blob, filename) {
        if (window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            const a = document.createElement('a');
            document.body.appendChild(a);
            const url = window.URL.createObjectURL(blob);
            a.href = url;
            a.download = filename;
            a.click();
            setTimeout(() => {
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                resetForm();
            }, 0);
        }
    }

    // Init Methods
    $(function () {
        initReset();
        initForm();
    });
})(jQuery);

/**
 * Emulate FormData for some browsers
 * MIT License
 * (c) 2010 François de Metz
 */
(function (w) {
    if (w.FormData) {
        return;
    }

    function FormData() {
        this.fake = true;
        this.boundary = '--------FormData' + Math.random();
        this._fields = [];
    }

    FormData.prototype.append = function (key, value) {
        this._fields.push([key, value]);
    };

    FormData.prototype.toString = function () {
        var boundary = this.boundary;
        var body = '';
        this._fields.forEach(function (field) {
            body += '--' + boundary + '\r\n';
            // file upload
            if (field[1].name) {
                var file = field[1];
                body += 'Content-Disposition: form-data; name=\'' + field[0] + '\'; filename=\'' + file.name + '\'\r\n';
                body += 'Content-Type: ' + file.type + '\r\n\r\n';
                body += file.getAsBinary() + '\r\n';
            } else {
                body += 'Content-Disposition: form-data; name=\'' + field[0] + '\';\r\n\r\n';
                body += field[1] + '\r\n';
            }
        });
        body += '--' + boundary + '--';
        return body;
    };

    w.FormData = FormData;

})(window);