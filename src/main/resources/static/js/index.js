(function ($) {

    // Init Reset Button
    function initReset() {
        $('body').on('click', '.btn-reset', function (e) {
            e.preventDefault();

            var form = $('#pdfForm').trigger('reset');
            $('#file1').change();
            // Remove previous hidden fields
            $('.fileOrder').remove();
        });
    }

    // Init Sortable for files
    function initFilesTable() {
        $('body').on('change', '#file1', function (e) {
            var inp = $(this);
            var files = inp[0].files;

            var filesTable = $('#filesTable');
            var filesBody = $('#filesBody');

            // Empty the current file list
            filesBody.empty();

            // Generate new list
            var newList = [];

            for (var i = 0; i < files.length; i++) {
                var file = files[i];

                var a = '<tr data-id="' + i + '" data-filename="' + file.name + '">' +
                        '<td><i class="fa fa-bars"></i></td>' +
                        '<td>' + file.name + '</td>' +
                        '<td>' + file.type + '</td>' +
                        '</tr>';
                newList.push(a);
            }

            if (newList.length > 0) {
                filesBody.append(newList);
                filesTable.removeClass('hidden');
            } else {
                filesTable.addClass('hidden');
            }

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
                    filesBody.find('tr').each(function (i, item) {
                        console.log(i, item);
                        $(item).data('id', i);
                    });
                });
    }

    function initForm() {
        var form = $('#pdfForm');
        var filesBody = $('#filesBody');

        form.on('submit', function (e) {
            // Remove previous hidden fields
            $('.fileOrder').remove();

            filesBody.find('tr').each(function (i, item) {
                var f = $(item);
                form.append('<input type="hidden" class="fileOrder" name="' + f.data('filename') + '" value="' + f.data('id') + '" />');
            });

            return true;
        });
    }

    $("#pdfForm").bind('ajax:complete', function () {
        console.log(arguments);
    });

    // Init Methods
    $(function () {
        initReset();
        initSortable();
        initFilesTable();
        initForm();
    });
})(jQuery);