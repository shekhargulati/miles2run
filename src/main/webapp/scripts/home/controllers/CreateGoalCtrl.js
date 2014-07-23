'use strict';

function CreateGoalCtrl($scope, $location, activeProfile, $http, ConfigService, $window) {

    $scope.currentUser = activeProfile;

    $scope.goal = {
        goalUnit: 'MI',
        numberOfDays: 10,
        startDate: new Date(),
        purpose: 'Run for 10 days'
    };

    $scope.buttonText = "Create";

    $scope.renderNextView = function () {
        console.log($scope.goalType);
        if ($scope.goalType === 'distance_goal') {
            $location.path("/goals/create_distance_goal");
        } else if ($scope.goalType === 'duration_goal') {
            $location.path("/goals/create_duration_goal");
        } else {
            $location.path("/goals/create_distance_goal");
        }

    }

    $scope.createDistanceGoal = function () {
        $scope.submitted = true;
        if ($scope.goalForm.$valid) {
            createGoal('DISTANCE_GOAL');
        }
    }

    $scope.validateDateRange = function (startDate, endDate) {
        if (startDate && endDate) {
            if (startDate.getTime() < endDate.getTime()) {
                $scope.goalForm.startDate.$invalid = false;
            } else {
                $scope.goalForm.startDate.$invalid = true;
            }
        }
    }
    $scope.createDurationGoal = function () {
        $scope.submitted = true;
        $scope.validateDateRange($scope.goal.startDate, $scope.goal.endDate);
        if ($scope.goalForm.$valid && !$scope.goalForm.startDate.$invalid) {
            createGoal('DURATION_GOAL');
        }
    }

    var createGoal = function (goalType) {
        $scope.successfulSubmission = true;
        $scope.buttonText = "Creating Goal..";
        $scope.goal.goalType = goalType;
        delete $scope.goal.numberOfDays;
        $scope.createGoalPromise = $http.post(ConfigService.getBaseUrl() + "goals", $scope.goal).success(function (data) {
            toastr.success("Created new goal");
            $window.location.href = ConfigService.appContext() + 'goals/' + data.id;
        }).error(function (data, status) {
            toastr.error("Unable to create goal. Please try after sometime.");
            console.log("Error " + data);
            console.log("Status " + status)
            $location.path("/");
        });

    };

    $scope.today = function () {
        $scope.dt = new Date();
    };
    $scope.today();

    $scope.showWeeks = true;
    $scope.toggleWeeks = function () {
        $scope.showWeeks = !$scope.showWeeks;
    };

    $scope.clear = function () {
        $scope.dt = null;
    };

    // Disable weekend selection
    $scope.disabled = function (date, mode) {
        return ( mode === 'day' && ( date.getDay() === 0 || date.getDay() === 6 ) );
    };

    $scope.toggleMin = function () {
        $scope.minDate = ( $scope.minDate ) ? null : new Date();
    };
    $scope.toggleMin();

    $scope.openStartDate = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();

        $scope.openedStartDate = true;
    };

    $scope.openEndDate = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();

        $scope.openedEndDate = true;
    };

    $scope.dateOptions = {
        'year-format': "'yy'",
        'starting-day': 1
    };

    $scope.goal.endDate = addDays($scope.goal.startDate, $scope.goal.numberOfDays)

    $scope.numberOfDaysUpdated = function () {
        if ($scope.goal.numberOfDays) {
            $scope.goal.endDate = addDays($scope.goal.startDate, $scope.goal.numberOfDays);
        }
    }

    $scope.startDateUpdated = function () {
        $scope.goal.numberOfDays = moment($scope.goal.endDate).diff($scope.goal.startDate, 'days') + 1;
    }

    $scope.endDateUpdated = function () {
        $scope.goal.numberOfDays = moment($scope.goal.endDate).diff($scope.goal.startDate, 'days') + 1;
    }

    function addDays(date, days) {
        var result = new Date(date);
        result.setDate(date.getDate() + (days - 1));
        return result;
    }

    $scope.minStartDate = $scope.minStartDate ? null : new Date();
    $scope.minEndDate = $scope.minEndDate ? null : $scope.minStartDate;


}

angular.module('miles2run-home')
    .controller('CreateGoalCtrl', CreateGoalCtrl);
