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
                var contentType = file.type;

                var a = $('<tr data-id="' + i + '" data-filename="' + file.name + '">' +
                        '<td><i class="fa fa-bars"></i></td>' +
                        '<td class="fileName">' + file.name + ' <br/> <span class="loading-indecator"> Loading Previews &nbsp; <i class="fa fa-refresh fa-spin"></i></span> <div class="preview-gallery" style="display: none;"></div></td>' +
                        '<td class="fileType">' + contentType + '</td>' +
                        '<td><button class="btn btn-danger btn-remote-file" type="button" title="Remove"><i class="fa fa-trash"></i></button></td>' +
                        '</tr>');

                var fileData = {
                    order: i,
                    file: file,
                    fileName: file.name
                };

                a.data('fileData', fileData);

                console.log(file);

                newList.push(a);

                if (contentType.indexOf('pdf') >= 0) {
                    doPreview(fileData, a);
                } else {
                    a.find('.loading-indecator').hide();
                }
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

    function doPreview(fileData, tr) {
        console.log('doPreview', fileData, tr);

        // Fetch the previews
        var formData = new FormData();
        formData.append('file', fileData.file, fileData.fileName);

        var ajaxOpts = {
            type: 'POST',
            url: '/pdfThumbnail',
            data: formData,
            dataType: 'binary',
            processData: false,
            contentType: false,
            xhrFields: {
                responseType: 'json'
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
                console.log('success', resp);
                tr.find('.loading-indecator').hide();

                if (resp && resp.status) {
                    var data = resp.data;

                    var previews = [];

                    // preview-gallery
                    var galWrapper = tr.find('.preview-gallery');
                    galWrapper.empty();

                    galWrapper.append($('<div class="row">' +
                            '<div class="list-group gallery">' +
                            '</div>' +
                            '</div>'));

                    var gal = galWrapper.find('.gallery');

                    for (var i = 0; i < data.length; i++) {
                        var d = data[i];

                        var t = '<div class="col-lg-2">' +
                                '    <a class="thumbnail fancybox" rel="ligthbox" href="data:image/' + d.format + ';base64,' + d.encodedImg + '">' +
                                '        <img class="img-responsive" data-orig-page-num="' + i + '" src="data:image/' + d.format + ';base64,' + d.encodedImg + '" />' +
                                '    </a>' +
                                '</div>';

                        var thumb = $(t);
                        previews.push(thumb);
                    }

                    if (previews.length > 0) {
                        gal.append(previews);

                        galWrapper.show();
                        galWrapper.find(".fancybox").fancybox({
                            buttons: ['rotateleft', 'rotateright', 'close'],
                            protect: false,
                            padding: 0,
                            afterShow: function (instance, slide) {
                                console.log(instance, slide);

                                // Init rotate
                                var img = slide.$image;
                                var thumb = slide.opts.$thumb;

                                var degrees = thumb.data('current-degrees') || 0;

                                img.css('-webkit-transform', 'rotate(' + degrees + 'deg)');
                                img.css('-moz-transform', 'rotate(' + degrees + 'deg)');
                                img.css('-ms-transform', 'rotate(' + degrees + 'deg)');
                                img.css('-o-transform', 'rotate(' + degrees + 'deg)');

                                $('[data-fancybox-rotateleft]')
                                        .off('click')
                                        .on('click', function (e) {
                                            var degrees = thumb.data('current-degrees') || 0;
                                            degrees -= 90;

                                            thumb.data('current-degrees', degrees);

                                            console.log(img, thumb, degrees);

                                            img.css('-webkit-transform', 'rotate(' + degrees + 'deg)');
                                            img.css('-moz-transform', 'rotate(' + degrees + 'deg)');
                                            img.css('-ms-transform', 'rotate(' + degrees + 'deg)');
                                            img.css('-o-transform', 'rotate(' + degrees + 'deg)');

                                            var thumbWrapper = thumb.closest('.thumbnail');
                                            thumbWrapper.css('-webkit-transform', 'rotate(' + degrees + 'deg)');
                                            thumbWrapper.css('-moz-transform', 'rotate(' + degrees + 'deg)');
                                            thumbWrapper.css('-ms-transform', 'rotate(' + degrees + 'deg)');
                                            thumbWrapper.css('-o-transform', 'rotate(' + degrees + 'deg)');

                                            instance.update();
                                            instance.Thumbs.update();
                                        });

                                $('[data-fancybox-rotateright]')
                                        .off('click')
                                        .on('click', function (e) {
                                            var degrees = thumb.data('current-degrees') || 0;
                                            degrees += 90;

                                            thumb.data('current-degrees', degrees);

                                            console.log(img, thumb, degrees);

                                            img.css('-webkit-transform', 'rotate(' + degrees + 'deg)');
                                            img.css('-moz-transform', 'rotate(' + degrees + 'deg)');
                                            img.css('-ms-transform', 'rotate(' + degrees + 'deg)');
                                            img.css('-o-transform', 'rotate(' + degrees + 'deg)');

                                            var thumbWrapper = thumb.closest('.thumbnail');
                                            thumbWrapper.css('-webkit-transform', 'rotate(' + degrees + 'deg)');
                                            thumbWrapper.css('-moz-transform', 'rotate(' + degrees + 'deg)');
                                            thumbWrapper.css('-ms-transform', 'rotate(' + degrees + 'deg)');
                                            thumbWrapper.css('-o-transform', 'rotate(' + degrees + 'deg)');

                                            instance.update();
                                            instance.Thumbs.update();
                                        });
                            }
                        });
                    } else {
                        galWrapper.hide();
                    }
                }
            },
            error: function () {
                tr.find('.loading-indecator').hide();
                console.log('error');
            }
        };

        $.ajax(ajaxOpts);
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
                var settings = {
                    files: []
                };

                filesBody.find('tr').each(function (i, item) {
                    var f = $(item);
                    var d = f.data('fileData');
                    fileOrder.push(d);
                    var previewContainer = f.find('.gallery');

                    var pageBean = {
                        fileName: d.fileName,
                        order: d.order,
                        pages: []
                    };

                    previewContainer.find('.img-responsive').each(function (a, b) {
                        var img = $(b);

                        var origPageNum = img.data('orig-page-num');
                        var degrees = img.data('current-degrees');

                        console.log('Img', origPageNum, degrees);

                        pageBean.pages.push({
                            originalPageNum: origPageNum,
                            newPageNum: origPageNum,
                            rotate: degrees
                        });
                    });

                    settings.files.push(pageBean);
                });

                fileOrder.sort(dynamicSort('order'));
                settings.files.sort(dynamicSort('order'));

                var formData = new FormData();

                formData.append('outputName', $('#outputName').val());
                formData.append('settings', new Blob([JSON.stringify(settings)], {
                    type: "application/json"
                }));

                for (var i = 0; i < fileOrder.length; i++) {
                    var f = fileOrder[i];
                    formData.append('files', f.file, f.fileName);
                }

                var ajaxOpts = {
                    type: 'POST',
                    url: '/mergePdf',
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

    function initFancyBoxButtons() {
        $.fancybox.defaults.btnTpl.rotateleft = '<button data-fancybox-rotateleft class="fancybox-button fancybox-button--rotateleft" title="Rotate Image Left"><i class="fa fa-rotate-left"></i></button>';
        $.fancybox.defaults.btnTpl.rotateright = '<button data-fancybox-rotateright class="fancybox-button fancybox-button--rotateright" title="Rotate Image Right"><i class="fa fa-rotate-right"></i></button>';
    }

    // Init Methods
    $(function () {
        initFancyBoxButtons();
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