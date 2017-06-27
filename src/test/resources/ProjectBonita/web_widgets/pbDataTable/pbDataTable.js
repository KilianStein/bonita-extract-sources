(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('pbDataTable', function() {
    return {
      controllerAs: 'ctrl',
      controller: function PbDataTableCtrl($scope, $http, $log, $filter) {

  var vm = this;

  Object.defineProperty(vm, 'jsonData', {
    'get': function () {
      //undefined for filter expression allows to bypass the null value issue that
      //filters everything
      var data = $filter('filter')(this.data || [], $scope.properties.filter || undefined);
      return $filter('orderBy')(data, vm.sortOptions.property, vm.sortOptions.direction);
    },
    'set': function (data) {
      this.data = data;
    }
  });

  this.sortOptions = {
    property: ($scope.properties.sortColumns || [])[0],
    direction: false
  };

  this.pagination = {
    currentPage: 1,
    total: 0
  };

  /**
   * helper methods
   */
  this.hasMultiColumns = function () {
    return Array.isArray($scope.properties.columnsKey);
  };

  this.isSelectable = function () {
    return $scope.properties.isBound('selectedRow');
  };

  /**
   * Create a request object following $http(request)
   * @return {Object} a request Object
   */
  this.createRequest = function () {
    var params = {
      c: $scope.properties.pageSize,
      p: this.pagination.currentPage - 1 || 0,
      s: $scope.properties.filter
    };

    if (this.sortOptions.property) {
      params.o = this.sortOptions.property + ' ' + (this.sortOptions.direction ? 'DESC' : 'ASC');
    }
    return {
      url: this.removeHandledParams($scope.properties.apiUrl),
      transformResponse: $http.defaults.transformResponse.concat(transformResponse),
      params: angular.extend({}, $scope.properties.params || {}, params)
    };
  };

  this.removeHandledParams = function(url) {
    return ['c', 'p'].reduce(
      function(acc, param) {
        //we cannot use pbDataTable because the widget build (probably mustache) replace it
        // with the widget name...
        return acc.replace(new RegExp('(&' + param + '=[^&#]*([&#])?)', 'g'), '$2').replace(new RegExp('[?]' + param + '=[^&#]*&?', 'g'), '?');
      },
      url || '');
  };

  /**
   *  Fire request and update results and pagination
   */
  this.updateResultsFromAPI = function () {
    if($scope.properties.apiUrl) {
      $http(vm.createRequest())
        .then(function (response) {
          vm.results = response.data.results;
          vm.pagination = response.data.pagination;
        })
        .catch(function (error) {
          $log.error(error);
        });
    }
  };

  this.updateResultsFromJson = function () {
    var start = (vm.pagination.currentPage - 1) * $scope.properties.pageSize;
    var end = vm.pagination.currentPage * $scope.properties.pageSize;
    vm.results = vm.jsonData.slice(start, end);
  };


  this.sortHandler = function () {
    this.updateResults();
  };

  this.paginationHandler = function () {
    this.updateResults();
  };

  this.selectRowHandler = function (row) {
    if (this.isSelectable()) {
      $scope.properties.selectedRow = row;
    }
  };

  this.isColumnSortable = function(index) {
    return $scope.properties.type === 'Variable' ||
      (angular.isArray($scope.properties.sortColumns) &&
        $scope.properties.sortColumns.indexOf($scope.properties.columnsKey[index]) > -1);
  };

  function transformResponse(data, header) {
    return {
      results: data,
      pagination: parseContentRange(header('Content-Range'))
    };
  }

  /**
   * helper method which extract pagination data from Content-range HTTP header
   * @param  {String} strContentRange Content-Range value
   * @return {Object}                 object containing pagination
   */
  function parseContentRange(strContentRange) {
    if (strContentRange === null) {
      return {};
    }
    var arrayContentRange = strContentRange.split('/');
    var arrayIndexNumPerPage = arrayContentRange[0].split('-');
    return {
      total: parseInt(arrayContentRange[1], 10),
      currentPage: parseInt(arrayIndexNumPerPage[0], 10) + 1
    };
  }

  vm.updateResults = function () {
    if ($scope.properties.type === 'Variable') {
      vm.updateResultsFromJson();
    } else {
      vm.updateResultsFromAPI();
    }
  };

  //watchGroup does not support object equality so we use another way to monitor all at once
  $scope.$watch('[properties.pageSize, properties.apiUrl, properties.filter, properties.params]', resetPaginationAndUpdateResults, true);

  function resetPaginationAndUpdateResults() {
    vm.pagination = {
      currentPage: 1,
      total: vm.jsonData.length
    };
    vm.updateResults();
  }

  $scope.$watchCollection('properties.content', function (data) {
    if (!Array.isArray(data)) {
      return;
    }
    vm.jsonData = data;
    vm.pagination = {
      currentPage: 1,
      total: data.length
    };
    vm.updateResults();
  });
}
,
      template: '<div class="table-responsive">\n    <table bonitable\n           sort-options="ctrl.sortOptions"\n           on-sort="ctrl.sortHandler()"\n           class="table table-striped" ng-class="{\'table-hover\': ctrl.isSelectable()}">\n        <thead>\n            <tr>\n                <th ng-repeat="header in properties.headers">\n                    <div ng-if="ctrl.isColumnSortable($index)" bo-sorter="{{properties.columnsKey[$index]}}">\n                            {{ header | uiTranslate }}\n                    </div>\n                    <div ng-if="!ctrl.isColumnSortable($index)">\n                        {{ header | uiTranslate }}\n                    </div>\n                </th>\n            </tr>\n        </thead>\n        <tbody ng-if="ctrl.hasMultiColumns()">\n        <tr ng-repeat="row in ctrl.results" ng-click="ctrl.selectRowHandler(row)"\n            ng-class="{\'info\': row === properties.selectedRow}">\n            <td ng-repeat="column in properties.columnsKey track by $index">\n                {{ $eval(column, row) | uiTranslate }}\n            </td>\n        </tr>\n        </tbody>\n        <tbody ng-if="!ctrl.hasMultiColumns()">\n        <tr ng-repeat="row in ctrl.results" ng-click="ctrl.selectRowHandler(row)"\n            ng-class="{\'info\': row === properties.selectedRow}">\n            <td> {{ row | uiTranslate }}</td>\n        </tr>\n        </tbody>\n    </table>\n    <pagination ng-if="ctrl.pagination.total > properties.pageSize" total-items="ctrl.pagination.total" items-per-page="properties.pageSize" direction-links="false" boundary-links="false"\n                ng-model="ctrl.pagination.currentPage" ng-change="ctrl.paginationHandler()"></pagination>\n</div>\n'
    };
  });
