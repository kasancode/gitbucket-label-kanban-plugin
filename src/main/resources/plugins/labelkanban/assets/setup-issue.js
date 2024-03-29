
var LabelKanbanPlugin = {
    setupLabel: function (id, labelSetupPath) {
        if (!labelSetupPath)
            return;
        var $element = $('a.toggle-label[data-label-id="' + id + '"]');

        this.switchLabel($element);
        var labelNames = Array();

        $('a.toggle-label').each(function (i, e) {
            if ($(e).children('i').hasClass('octicon-check') == true) {
                labelNames.push($(e).text().trim());
            }
        });
        $('input[name=labelNames]').val(labelNames.join(','));

        $.post(labelSetupPath,
            { labelNames: labelNames.join(',') },
            function (data) {
                $('ul.label-list').empty().html(data);
            }
        );
    }
    ,
    setupMilestone: function (id) {
        var $element = $('a.milestone[data-id="' + id + '"]');
        var title = $element.data('title');
        var milestoneId = $element.data('id');
        this.displayMilestone(title, milestoneId);
        $('input[name=milestoneId]').val(milestoneId);
    }
    ,
    setupPriority: function (id) {
        var $element = $('a.priority[data-id="' + id + '"]');
        var priorityName = $element.data('name');
        var priorityId = $element.data('id');
        var description = $element.attr('title');
        var color = $element.data('color');
        var fontColor = $element.data('font-color');
        this.displayPriority(priorityName, priorityId, description, color, fontColor);
        $('input[name=priorityId]').val(priorityId);
    }
    ,
    setupAssignee: function (name) {
      $element = $('a.toggle-assign[data-name="' + name + '"]');

      const i = $element.children('i');
      if(i.hasClass('octicon-check')){
        i.removeClass('octicon-check');
      } else {
        i.addClass('octicon-check');
      }

      const assignees = Array();
      $('a.toggle-assign').each(function(i, e){
        if($(e).children('i').hasClass('octicon-check') == true){
          assignees.push($(e).text().trim());
        }
      });
      $('input[name=assigneeUserNames]').val(assignees.join(','));
      this.displayAssignee(assignees);
    }
    ,
    switchLabel: function ($this) {
        var i = $this.children('i');
        if (i.hasClass('octicon-check')) {
            i.removeClass('octicon-check');
            return 'delete';
        } else {
            i.addClass('octicon-check');
            return 'new';
        }
    }
    ,
    displayMilestone: function (title, milestoneId, progress) {
        $('a.milestone i.octicon-check').removeClass('octicon-check');
        if (milestoneId == '') {
            $('#label-milestone').html($('<span class="muted small">').text('No milestone'));
            $('#milestone-progress-area').empty();
        } else {
            $('#label-milestone').html($('<a class="strong small username">').text(title)
                .attr('href', '/root/test6/issues?milestone=' + encodeURIComponent(title) + '&state=open'));
            if (progress) {
                $('#milestone-progress-area').html(progress);
            }
            $('a.milestone[data-id=' + milestoneId + '] i').addClass('octicon-check');
        }
    }
    ,
    displayPriority: function (priorityName, priorityId, description, color, fontColor) {
        $('a.priority i.octicon-check').removeClass('octicon-check');
        if (priorityId == '') {
            $('#label-priority').html($('<span class="muted small">').text('No priority'));
        } else {
            $('#label-priority').html($('<a class="issue-priority">').text(priorityName)
                .attr('href', '/root/test6/issues?priority=' + encodeURIComponent(priorityName) + '&state=open')
                .attr('title', description)
                .css({
                    "background-color": color,
                    "color": fontColor
                }));

            $('a.priority[data-id=' + priorityId + '] i').addClass('octicon-check');
        }
    }
    ,
    displayAssignee: function (assignees) {
      $('a.assign i.octicon-check').removeClass('octicon-check');
      if(assignees.length == 0){
        $('#label-assigned').html($('<span class="muted small">').text('No one assigned'));
      } else {
        $('#label-assigned').empty();
        for (const userName of assignees) {
          $('#label-assigned').append($('<div>').append(
              $('a.toggle-assign').parent().find("img.avatar-mini[alt='@" + userName + "']").clone(false),
              ' ',
              $('<a class="username strong small">').attr('href', '/' + userName).text(userName)));
        }
      }
    }
};
