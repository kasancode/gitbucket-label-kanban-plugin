

var apiBasepath;
var basePath;
var prefix;
var addIssuePath;
var closeIssuePath;

const compactStyleIssuesCount = 10;
const cookieMaxAge = 30; //day

// Polyfill
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/startsWith
if (!String.prototype.startsWith) {
    Object.defineProperty(String.prototype, 'startsWith', {
        value: function (search, pos) {
            pos = !pos || pos < 0 ? 0 : +pos;
            return this.substring(pos, pos + search.length) === search;
        }
    });
}

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/find
if (!Array.prototype.find) {
    Object.defineProperty(Array.prototype, 'find', {
        value: function (predicate) {
            if (this == null) {
                throw new TypeError('"this" is null or not defined');
            }
            var o = Object(this);
            var len = o.length >>> 0;
            if (typeof predicate !== 'function') {
                throw new TypeError('predicate must be a function');
            }
            var thisArg = arguments[1];
            var k = 0;
            while (k < len) {
                var kValue = o[k];
                if (predicate.call(thisArg, kValue, k, o)) {
                    return kValue;
                }
                k++;
            }
            return undefined;
        },
        configurable: true,
        writable: true
    });
}

// https://developer.mozilla.org/ja/docs/Web/JavaScript/Reference/Global_Objects/Array/findIndex
if (!Array.prototype.findIndex) {
    Array.prototype.findIndex = function(predicate) {
      if (this === null) {
        throw new TypeError('Array.prototype.findIndex called on null or undefined');
      }
      if (typeof predicate !== 'function') {
        throw new TypeError('predicate must be a function');
      }
      var list = Object(this);
      var length = list.length >>> 0;
      var thisArg = arguments[1];
      var value;
  
      for (var i = 0; i < length; i++) {
        value = list[i];
        if (predicate.call(thisArg, value, i, list)) {
          return i;
        }
      }
      return -1;
    };
  }

function getCookie(name) {
    var cookieName = encodeURIComponent(name) + '=';
    var allcookies = document.cookie;

    var position = allcookies.indexOf(cookieName);
    if (position < 0) {
        return null;
    }

    var startIndex = position + cookieName.length;

    var endIndex = allcookies.indexOf(';', startIndex);
    if (endIndex < 0) {
        endIndex = allcookies.length;
    }

    return decodeURIComponent(
        allcookies.substring(startIndex, endIndex));
}

function setCookie(name, value) {
    var maxAge = 60 * 60 * 24 * cookieMaxAge;
    document.cookie = encodeURIComponent(name) + "=" + encodeURIComponent(value) + "; max-age=" + maxAge.toString();
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
        /**@returns {Object} */
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
            if (!this.lanes || !key || !this.lanes[key])
                return null;

            if (dummyFirst)
                return this.lanes[key];
            else
                return this.lanes[key].slice().reverse();
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
         * @param {string} key
         * @param {string} value
         * @returns {string}
         */
        iconClass: function (key, value) {
            var lane = this.lanes[key].find(function(l){return l.id === value;});
            return lane ? lane.icon : "";
        }
        ,
        /**
         * @param {string} key
         * @param {string} value
         * @returns {string}
         */
        iconColor: function (key,value) {
            var lane = this.lanes[key].find(function(l){return l.id === value;});
            return lane ? "#" + lane.color : "#333333";
        }
        ,
        /**
         * @param {string} key
         * @param {string} value
         * @returns {string}
         */
        laneName: function (key,value) {
            var lane = this.lanes[key].find(function(l){return l.id === value;});
            return lane ? lane.name : "";
        }
        ,
        /**
         * @param {string} key
         * @param {string} value
         * @returns {string}
         */
        laneUrl: function (key,value) {
            var lane = this.lanes[key].find(function(l){return l.id === value;});
            return lane ? lane.htmlUrl : "";
        }
        ,
        /**
         * @param {string} key
         * @param {string} value
         * @returns {string}
         */
        laneImageUrl: function (key,value) {
            var lane = this.lanes[key].find(function(l){return l.id === value;});
            return lane ? lane.iconImage : "";
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
            return lane ? lane.htmlUrl : "";
        }
        ,
        /**@returns {Object} */
        getContainerStyle: function () {
            return {
                "display": "grid",
                "grid-template-rows": "100px 50px",
                "grid-template-columns": "150px 1fr"
            };
        }
        ,
        /**@returns {boolean} */
        isCompact: function () {
            return this.issues.length > compactStyleIssuesCount;
        }
    }
    ,
    methods: {
        saveCookie: function () {
            setCookie("kanban.rowKey", this.rowKey);
            setCookie("kanban.colKey", this.colKey);
            for (var key in this.lanes) {
                setCookie("kanban.order." + key, JSON.stringify(this.lanes[key].map(function (item) {
                    return { id: item.id, order: item.order };
                })));
            }
        }
        ,
        loadCookie: function () {
            this.rowKey = getCookie("kanban.rowKey") || this.rowKey;
            this.colKey = getCookie("kanban.colKey") || this.colKey;

            for (var key in this.lanes) {
                var orders = JSON.parse(getCookie("kanban.order." + key));
                if (!orders || !orders.map) {
                    continue;
                }

                orders.map(function (item) {
                    var targetLane = this.lanes[key].find(function (lane) {
                        return lane.id == item.id;
                    });
                    if (!!targetLane) {
                        targetLane.order = item.order;
                    }
                }, this);

                this.lanes[key].sort(function (a, b) {
                    return a.order - b.order;
                });

                // fix irregular condition
                for(var i=0;i<this.lanes[key].length;i++){
                    this.lanes[key][i].order = i;
                }
            }
        }
        ,
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

            if (this.colKey != this.rowKey || !changed)
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
            var isTarget = rowLane && colLane &&
                (this.targetColLane && this.targetColLane.name == colLane.name) &&
                (this.targetRowLane && this.targetRowLane.name == rowLane.name)
            return {
                'background-color': (isTarget ? "#f5f5f5" : "white"),
            };
        }
        ,
        /**
         * @param {lane} rowLane
         * @param {lane} colLane
         * @returns {String}
         */
        getNewIssueUrl: function (rowLane, colLane) {
            if (!addIssuePath) return null;

            var url = addIssuePath + "?";
            if (rowLane.paramKey)
                url += rowLane.paramKey + "=" + encodeURIComponent(rowLane.id);
            if (rowLane.paramKey && colLane.paramKey)
                url += "&";
            if (colLane.paramKey)
                url += colLane.paramKey + "=" + encodeURIComponent(colLane.id);
            return url;
        }
        ,
                /**
         * @param {issue} issue
         */
        closeIssue: function (issue) {
            if (!closeIssuePath) return;

            $.ajax({
                url: closeIssuePath + issue.issueId,
                dataType: 'json'
            })
                .done(function () {
                    var index = this.issues.findIndex(function(i){return i.issueId === issue.issueId;});
                    if(index >= 0){
                        this.issues.splice(index, 1);
                        this.$forceUpdate();
                    }
                }.bind(this))
                .fail(this.ajaxFail);
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
        loadDataSet: function () {
            $.ajax({
                url: basePath + 'dataset',
                dataType: 'json'
            })
                .done(function (data) {
                    this.lanes = data.lanes;
                    this.issues = data.issues;

                    this.colKey = this.getLaneKeys()[1];
                    this.rowKey = this.getLaneKeys()[0];

                    this.loadCookie();
                    this.saveCookie();

                    this.$forceUpdate();
                }.bind(this))
                .fail(this.ajaxFail);
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

            if (targetLane.switchUrl) {
                $.ajax({
                    url: targetLane.switchUrl + issue.issueId
                })
                    .fail(this.ajaxFail);
            }

            issue.metrics[key] = targetLane.id;
            return true;
        }
        ,
        /**
         * @param {string} key 
         * @param {lane} lane
         * @param {boolean} direction
         */
        changeLaneOrder: function (key, lane, direction) {
            var target = null;
            if (direction) {
                // +
                var min = Number.MAX_VALUE;
                for (var i = 0; i < this.lanes[key].length; i++) {
                    if (lane.order < this.lanes[key][i].order && this.lanes[key][i].order < min) {
                        min = this.lanes[key][i].order;
                        target = this.lanes[key][i];
                    }
                }
            }
            else {
                // -
                var max = -Number.MAX_VALUE;
                for (var i = 0; i < this.lanes[key].length; i++) {
                    if (max < this.lanes[key][i].order && this.lanes[key][i].order < lane.order) {
                        max = this.lanes[key][i].order;
                        target = this.lanes[key][i];
                    }
                }
            }

            if (target === null) {
                return;
            }

            var tmp = target.order;
            target.order = lane.order;
            lane.order = tmp;

            this.lanes[key].sort(function (a, b) {
                return a.order - b.order;
            });

            this.$forceUpdate();

            this.saveCookie();
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
                    this.$forceUpdate();
                }.bind(this))
                .fail(this.ajaxFail);
        }
        ,
        /**
         */
        ajaxFail: function (jqXHR, textStatus, errorThrown) {
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
                    this.loadDataSet();
                    this.toggleLabelEditor();
                }.bind(this))
                .fail(this.ajaxFail);
        }
        ,
        toggleLabelEditor: function () {
            this.showNewLabelEditor = !this.showNewLabelEditor;
            this.newLabelName = "";
            this.newLabelColor = "#888888";
            this.message = "";
        }
    }
    ,
    updated: function(){
        $('[data-toggle="tooltip"]').tooltip();
    }
});

$(function () {
    kanbanApp.prefix = prefix;

    $('#kanban-new-label-color-holder').colorpicker({ format: "hex" })
        .on('changeColor', function (event) {
            kanbanApp.newLabelColor = event.color.toString();
        });

    kanbanApp.loadDataSet();
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
 * @prop {string} htmlUrl
 * @prop {string} avatar_url
 */

/**
 * @typedef {Object} label
 * @prop {string} userName
 * @prop {number} labelId
 * @prop {string} labelName
 * @prop {string} color
 * @prop {string} htmlUrl
 * @prop {string} switchUrl
 */

/**
* @typedef {Object} lane
* @prop {Object} id
* @prop {string} name
* @prop {string} color
* @prop {string} htmlUrl
* @prop {string} switchUrl
* @prop {string} paramKey
* @prop {number} order
* @prop {string} iconImage
* @prop {string} icon
*/

/**
 * @typedef {Object} comment
 * @prop {number} id
 * @prop {user} user
 * @prop {string} body
 * @prop {string} created_at
 * @prop {string} updated_at
 * @prop {string} htmlUrl
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
 * @prop {string} htmlUrl
 * @prop {string} comments_url
 *
 * @prop {boolean} show //optional
 * @prop {comment[]} comments //optional
 * @prop {object} metrics // optional
 */

/**
 * @typedef {Object} dataset
 * @prop {issue[]} issues
 * @prop {Object} metrics
 * @prop {Lane[]} lanes
 */