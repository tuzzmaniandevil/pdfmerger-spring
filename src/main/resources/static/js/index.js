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
        // Remove previous hidden fields
        $('.fileOrder').remove();
        $('#filesBody').empty();

        updateSorting();
    }

    function initRemoveFile() {
        $('body').on('click', '.btn-remote-file', function (e) {
            e.preventDefault();

            var btn = $(this);
            var tr = btn.closest('tr');

            if (confirm('Are you sure you want to remove this file?')) {
                tr.remove();
                updateSorting();
            }
        });
    }

    // Init Sortable for files
    function initFilesTable() {
        $('body').on('change', '#files', function (e) {
            var inp = $(this);
            var files = inp[0].files;

            var filesBody = $('#filesBody');

            // Generate new list
            var newList = [];

            for (var i = 0; i < files.length; i++) {
                var file = files[i];

                var a = $('<tr data-id="' + i + '" data-filename="' + file.name + '">' +
                        '<td><i class="fa fa-bars"></i></td>' +
                        '<td>' + file.name + '</td>' +
                        '<td>' + file.type + '</td>' +
                        '<td><button class="btn btn-danger btn-remote-file" type="button" title="Remove"><i class="fa fa-trash"></i></button></td>' +
                        '</tr>');

                var fileData = {
                    order: i,
                    file: file,
                    fileName: file.name
                };

                a.data('fileData', fileData);

                newList.push(a);
            }

            if (newList.length > 0) {
                filesBody.append(newList);
            }

            updateSorting();
            inp.val('');
        });
    }

    // Init Sortable for files
    function initSortable() {
        var filesBody = $('#filesBody');

        filesBody.disableSelection();
        filesBody
                .sortable({
                    axis: 'y'
                })
                .on('sortstop', function (event, ui) {
                    updateSorting();
                });
    }

    function initForm() {
        var form = $('#pdfForm');
        var filesBody = $('#filesBody');

        form.on('submit', function (e) {
            e.preventDefault();

            if (getFileCount() <= 0) {
                return false;
            } else {
                disableForm(true);
                var fileOrder = [];

                filesBody.find('tr').each(function (i, item) {
                    var f = $(item);
                    var d = f.data('fileData');
                    fileOrder.push(d);
                });

                fileOrder.sort(dynamicSort('order'));

                var formData = new FormData();

                formData.append('outputName', $('#outputName').val());

                for (var i = 0; i < fileOrder.length; i++) {
                    var f = fileOrder[i];
                    formData.append('files', f.file, f.fileName);
                }

                var ajaxOpts = {
                    type: 'POST',
                    url: '/upload',
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
                            filename = 'output.pdf';
                        }
                        saveFile(resp, filename);
                        disableForm(false);
                        swal("Success!", "Your files have been converted", "success");
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

    function updateSorting() {
        var filesTable = $('#filesTable');
        var filesBody = $('#filesBody');

        filesBody.find('tr').each(function (i, item) {
            var f = $(item);
            var d = f.data('fileData');
            d.order = i;

            console.log(i, d);
        });

        console.log('updateSorting', getFileCount());

        if (getFileCount() > 0) {
            filesTable.removeClass('hidden');
        } else {
            filesTable.addClass('hidden');
        }
    }

    function getFileCount() {
        return $('#filesBody').find('tr').length;
    }

    function dynamicSort(property) {
        var sortOrder = 1;
        if (property[0] === "-") {
            sortOrder = -1;
            property = property.substr(1);
        }
        return function (a, b) {
            var result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
            return result * sortOrder;
        };
    }

    // Init Methods
    $(function () {
        initReset();
        initSortable();
        initFilesTable();
        initForm();
        initRemoveFile();
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