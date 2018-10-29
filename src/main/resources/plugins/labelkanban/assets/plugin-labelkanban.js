

//var apiBasepath;
//var basePath;
//var prefixes;

var dummyLanes = {
    "None": {
        id: -1,
        name: "",
        color: "333333",
        html_url: "",
        detach_url: "",
        attach_url: ""
    }
}

function initilizeDummyLanes() {
    prefixes.map(function (prefix) {
        dummyLanes[prefixToLaneKey(prefix)] = {
            id: -1,
            name: "",
            color: "333333",
            html_url: "",
            detach_url: "",
            attach_url: ""
        }
    });
    dummyLanes["Milestones"] = {
        id: -1,
        name: "",
        color: "333333",
        detach_url: "",
        attach_url: basePath + "milestone/0/switch/issue/"
    };
    dummyLanes["Priorities"] = {
        id: -1,
        name: "",
        color: "333333",
        detach_url: "",
        attach_url: basePath + "priority/0/switch/issue/"
    };
    dummyLanes["Assignees"] = {
        id: "",
        name: "",
        color: "333333",
        detach_url: "",
        attach_url: basePath + "assignee/detach/issue/"
    };
}

if (!String.prototype.startsWith) {
    String.prototype.startsWith = function (prefix) {
        return this.lastIndexOf(prefix, 0) === 0;
    }
}

/**
 * 
 * @param {string} prefix 
 * @returns {string}
 */
function prefixToLaneKey(prefix) {
    return "Label:" + prefix;
}

var kanbanApp = new Vue({
    el: "#kanban-app",
    data: {
        message: ""
        ,
        /**@type {issue}*/
        dragItem: undefined
        ,
        /**@type {lane} */
        targetRowLane: undefined
        ,
        /**@type {lane} */
        targetColLane: undefined
        ,
        /**@type {lane} */
        originRowLane: undefined
        ,
        /**@type {lane} */
        originColLane: undefined
        ,
        /**@type {issue[]}*/
        issues: []
        ,
        showNewLabelEditor: false
        ,
        /**@type {string} */
        newLabelName: ""
        ,
        /**@type {string} */
        newLabelColor: "#888888"
        ,
        /**@type {Object.<string, lane[]}>} */
        lanes: {}
        ,
        /**@type {string} */
        colKey: ""
        ,
        /**@type {string} */
        rowKey: ""
        ,
        /**@type {string} */
        prefix: ""
        ,
        /**@type {string[]} */
        prefixes: []
        ,
        getLaneKeys: function () {
            return Object.keys(this.lanes);
        }
        ,
        /**
         * @param {string} key
         * @param {boolean} dummyFirst
         * @returns {lane[]}
         */
        getLanes: function (key, dummyFirst) {
            if (!key)
                return [dummyLanes["None"]];

            if (this.lanes[key].length < 1)
                return [dummyLanes["None"]];


            if (dummyFirst)
                return [dummyLanes[key]].concat(this.lanes[key]);
            else
                return this.lanes[key].slice().reverse().concat([dummyLanes[key]]);
        }
        ,
        /**
         * @param {issue[]} issues
         * @param {string} key
         * @param {lane} lane
         * @returns {issue[]}
         */
        getIssues: function (issues, key, lane) {
            return issues.filter(function (i) {
                return i.metrics[key] == lane.id;
            });
        }
        ,
        /**
         * @returns {string}
         */
        getColWidth: function () {
            var width = (100 - 2) / this.lanes[this.colKey].length - 2;
            return Math.round(width) + "%"
        }
        ,
        /**@param {lane} lane */
        getLaneUrl: function (lane) {
            return lane.html_url;
        }
        ,
        getContainerStyle: function () {
            return {
                "display": "grid",
                "grid-template-rows": "100px 50px",
                "grid-template-columns": "150px 1fr"
            };
        }
    }
    ,
    methods: {
        /**
         * @param {issue} issue
         * @param {Lane} row
         * @param {Lane} col
         * @param {DragEvent} e
         */
        dragstart: function (issue, row, col, e) {
            if (!issue || !issue.title) return;

            this.draggingItem = issue
            this.originRowLane = row;
            this.originColLane = col;
            e.target.style.opacity = 0.5;
        }
        ,
        /**
         * @param {DragEvent} e
         */
        dragend: function (e) {
            e.target.style.opacity = 1;
            var changed = this.changeLane(this.colKey, this.draggingItem, this.originColLane, this.targetColLane);

            if(this.colKey != this.rowKey || !changed)
                this.changeLane(this.rowKey, this.draggingItem, this.originRowLane, this.targetRowLane);

            this.draggingItem = undefined;
            this.targetColLane = undefined;
            this.targetRowLane = undefined;
            this.originColLane = undefined;
            this.originRowLane = undefined;
        }
        ,
        /**
         * @returns {boolean}
         */
        dragenter: function (rowLane, colLane) {
            if (!this.draggingItem) return;

            event.preventDefault();
        }
        ,
        /**
         * @param {lane} lane
         */
        dragover: function (rowLane, colLane) {
            if (!this.draggingItem) return;

            this.targetColLane = colLane;
            this.targetRowLane = rowLane;

            event.preventDefault();
        }
        ,
        /**
         * @param {issue} issue
         */
        showComments: function (issue) {
            issue.show = !issue.show;
            if (!issue.comments) {
                this.loadComments(issue);
            }
        }
        ,
        /**
         * @param {lane} rowLane
         * @param {lane} colLane
         * @returns {object}
         */
        getLaneStyle: function (rowLane, colLane) {
            var isTarget = rowLane && colLane && (this.targetColLane && this.targetColLane.name == colLane.name) &&
                (this.targetRowLane && this.targetRowLane.name == rowLane.name)
            return {
                'background-color': (isTarget ? "#f5f5f5" : "white"),
            };
        }
        ,
        /**
         * @param {lane} lane
         * @returns {object}
         */
        getLaneHeaderStyle: function (lane) {
            var color = (lane && lane.color) ? "#" + lane.color : "333333";
            return {
                'border-top': 'solid 7px ' + color
            }
        }
        ,
        /**
         * @param {Object} metrics
         * @param {string[]} labels
         */
        addLabelToMetrics: function (metrics, labels) {
            prefixes.forEach(function (prefix) {
                for (var i = 0; i < labels.length; i++) {
                    var label = labels[i];
                    if (label.startsWith(prefix)) {
                        var item = kanbanApp.lanes[prefixToLaneKey(prefix)].filter(function (l) {
                            return l.name == label;
                        });

                        var id = (item && item[0] && item[0].id) ? item[0].id : -1;
                        metrics[prefixToLaneKey(prefix)] = id;
                        break;
                    }
                }

                if (Object.keys(metrics).indexOf(prefixToLaneKey(prefix)) < 0)
                    metrics[prefixToLaneKey(prefix)] = -1;
            })
        }
        ,
        loadIssues: function () {
            var _this = this;

            $.ajax({
                url: basePath + 'issues',
                dataType: 'json'
            })
                .done(function (data) {
                    _this.issues = data.filter(function (issue) {
                        issue.show = false;
                        issue.comments = null;
                        issue.metrics = {};

                        issue.metrics["None"] = -1;
                        _this.addLabelToMetrics(issue.metrics, issue.labelNames)
                        issue.metrics["Milestones"] = issue.milestoneId;
                        issue.metrics["Priorities"] = issue.priorityId;
                        issue.metrics["Assignees"] = issue.assignedUserName;

                        return !issue.closed;
                    });

                    _this.colKey = _this.getLaneKeys()[1];
                    _this.rowKey = _this.getLaneKeys()[0];
                })
                .fail(this.ajaxFial);
        }
        ,
        loadLabels: function () {
            var _this = this;
            $.ajax({
                url: basePath + 'labels',
                dataType: 'json'
            })
                .done(function (data) {
                    for (var i = 0; i < prefixes.length; i++) {
                        var prefix = prefixes[i];

                        Vue.set(_this.lanes, prefixToLaneKey(prefix),
                            data.filter(function (label) {
                                return label.labelName.startsWith(prefix);
                            }).map(function (label) {
                                return {
                                    id: label.labelId,
                                    name: label.labelName,
                                    color: label.color,
                                    html_url: label.html_url,
                                    attach_url: label.attach_url,
                                    detach_url: label.detach_url
                                };
                            })
                        );
                    }

                    _this.$forceUpdate();
                })
                .fail(this.ajaxFial);
        }
        ,
        loadMilestones: function () {
            var _this = this;
            $.ajax({
                url: basePath + 'milestones',
                dataType: 'json'
            })
                .done(function (data) {
                    Vue.set(_this.lanes, "Milestones",
                        data.map(function (milestone) {
                            return {
                                id: milestone.milestoneId,
                                name: milestone.title,
                                color: "838383",
                                html_url: milestone.html_url,
                                attach_url: milestone.attach_url,
                                detach_url: milestone.detach_url
                            };
                        })
                    );
                })
                .fail(this.ajaxFial);
        }
        ,
        loadPriorities: function () {
            var _this = this;
            $.ajax({
                url: basePath + 'priorities',
                dataType: 'json'
            })
                .done(function (data) {
                    Vue.set(_this.lanes, "Priorities",
                        data.map(function (priority) {
                            return {
                                id: priority.priorityId,
                                name: priority.priorityName,
                                color: priority.color,
                                html_url: priority.html_url,
                                attach_url: priority.attach_url,
                                detach_url: priority.detach_url
                            };
                        })
                    );
                })
                .fail(this.ajaxFial);
        }
        ,
        loadAssignees: function () {
            var _this = this;
            $.ajax({
                url: basePath + 'assignees',
                dataType: 'json'
            })
                .done(function (data) {
                    Vue.set(_this.lanes, "Assignees",
                        data.map(function (assignee) {
                            return {
                                id: assignee.userName,
                                name: assignee.userName,
                                color: "838383",
                                html_url: assignee.html_url,
                                attach_url: assignee.attach_url,
                                detach_url: assignee.detach_url
                            };
                        })
                    );
                })
                .fail(this.ajaxFial);
        }
        ,
        /**
         * @param {string} key
         * @param {issue} issue 
         * @param {lane} originLane 
         * @param {lane} targetLane
         * @returns {boolean}
         */
        changeLane: function (key, issue, originLane, targetLane) {
            if (!issue || !originLane || !targetLane)
                return false;

            if (issue.metrics[key] == targetLane.id)
                return false;

            if (originLane.detach_url) {
                $.ajax({
                    url: originLane.detach_url + issue.issueId
                })
                    .fail(this.ajaxFial);
            }

            if (targetLane.attach_url) {
                $.ajax({
                    url: targetLane.attach_url + issue.issueId
                })
                    .fail(this.ajaxFial);
            }

            issue.metrics[key] = targetLane.id;
            return true;
        }
        ,
        /**
         * @param {issue} issue 
         */
        loadComments: function (issue) {

            $.ajax({
                url: issue.comments_url,
                dataType: 'json'
            })
                .done(function (data) {
                    issue.comments = data;
                })
                .fail(this.ajaxFial);
        }
        ,
        /**
         */
        ajaxFial: function (jqXHR, textStatus, errorThrown) {
            this.message = (jqXHR.responseJSON && jqXHR.responseJSON.message) ?
                jqXHR.responseJSON.message :
                textStatus;
        }
        ,
        addNewLabel: function () {
            if (!this.newLabelName || !this.newLabelColor) {
                this.message = "Label name and color are requred."
                return;
            }
            this.message = "";

            var _this = this;

            $.ajax({
                url: apiBasePath + 'labels',
                dataType: 'json',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    "name": this.prefix + this.newLabelName,
                    "color": this.newLabelColor.slice(1),
                })
            })
                .done(function (data) {
                    _this.loadLabels();
                    _this.toggleLabelEditor();
                })
                .fail(this.ajaxFial);
        }
        ,
        toggleLabelEditor: function () {
            this.showNewLabelEditor = !this.showNewLabelEditor;
            this.newLabelName = "";
            this.newLabelColor = "#888888";
            this.message = "";
        }
    }
});

$(function () {
    initilizeDummyLanes();
    kanbanApp.prefixes = prefixes;
    kanbanApp.prefix = prefixes[0];

    kanbanApp.lanes = {
        "None":[]
    };
    prefixes.map(function (prefix) {
        kanbanApp.lanes[prefixToLaneKey(prefix)] = [];
    });
    kanbanApp.lanes["Milestones"] = [];
    kanbanApp.lanes["Priorities"] = [];
    kanbanApp.lanes["Assignees"] = [];


    kanbanApp.loadLabels();
    kanbanApp.loadMilestones();
    kanbanApp.loadPriorities();
    kanbanApp.loadAssignees();
    kanbanApp.loadIssues();



    $('#kanban-new-label-color-holder').colorpicker({ format: "hex" })
        .on('changeColor', function (event) {
            kanbanApp.newLabelColor = event.color.toString();
        });
});

/**
 */

/**
 * @typedef {Object} user
 * @prop {string} login
 * @prop {string} email
 * @prop {string} type
 * @prop {boolean} site_admin
 * @prop {string} created_at
 * @prop {number} id
 * @prop {string} url
 * @prop {string} html_url
 * @prop {string} avatar_url
 */

/**
 * @typedef {Object} label
 * @prop {string} userName
 * @prop {number} labelId
 * @prop {string} labelName
 * @prop {string} color
 * @prop {string} html_url
 * @prop {string} detach_url
 * @prop {string} attach_url
 */

 /**
 * @typedef {Object} lane
 * @prop {Object} id
 * @prop {string} name
 * @prop {string} color
 * @prop {string} html_url
 * @prop {string} detach_url
 * @prop {string} attach_url
 */

/**
 * @typedef {Object} comment
 * @prop {number} id
 * @prop {user} user
 * @prop {string} body
 * @prop {string} created_at
 * @prop {string} updated_at
 * @prop {string} html_url
 */

/**
 * @typedef {Object} issue
 * @prop {string} userName
 * @prop {number} issueId
 * @prop {string} openedUserName
 * @prop {number} milestoneId
 * @prop {number} priorityId
 * @prop {string} assignedUserName
 * @prop {string} title
 * @prop {string} content
 * @prop {boolean} closed
 * @prop {Date} registeredDate
 * @prop {Date} updatedDate
 * @prop {boolean} isPullRequest
 * @prop {string[]]} labelNames
 * @prop {string} html_url 
 * @prop {string} comments_url
 * 
 * @prop {boolean} show //optional
 * @prop {comment[]} comments //optional
 * @prop {object} metrics // optional
 */

/**
 * @typedef {Object} milestone
 * @prop {string} userName
 * @prop {number} milestoneId
 * @prop {string} title
 * @prop {string} description
 * @prop {Date} dueDate
 * @prop {Date} closedDate
 * @prop {string} html_url
 * @prop {string} detach_url
 * @prop {string} attach_url
 */

/**
 * @typedef {Object} priority
 * @prop {string} userName
 * @prop {number} priorityId
 * @prop {string} priorityName
 * @prop {string} description
 * @prop {boolean} isDefault
 * @prop {number} ordering
 * @prop {string} color
 * @prop {string} html_url
 * @prop {string} detach_url
 * @prop {string} attach_url
*/

 /**
 * @typedef {Object} assignee
 * @prop {string} userName
 * @prop {string} html_url
 * @prop {string} detach_url
 * @prop {string} attach_url
 */