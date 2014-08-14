'use strict';

function PostActivityCtrl($scope, ActivityService, $location, ProfileService, activeProfile, $rootScope, activeGoal) {

    $scope.currentUser = activeProfile;
    $scope.activeGoal = activeGoal;

    $scope.buttonText = "Log your Run";

    $scope.activity = {
        goalUnit: $scope.activeGoal.goalUnit.$name
    };

    $scope.validateDuration = function (duration) {
        var durationVal = toAppSeconds(duration)
        if (durationVal >= 0) {
            $scope.activityForm.durationHours.$invalid = false;
        } else {
            $scope.activityForm.durationHours.$invalid = true;
        }
    }

    var toAppSeconds = function (duration) {
        if (duration) {
            var hours = duration.hours && duration.hours !== '00' ? duration.hours : 0;
            var minutes = duration.minutes && duration.minutes !== '00' ? duration.minutes : 0;
            var seconds = duration.seconds && duration.seconds !== '00' ? duration.seconds : 0;
            return Number(hours) * 60 * 60 + Number(minutes) * 60 + Number(seconds);
        }
        return 0;
    }

    $scope.postActivity = function () {
        $scope.submitted = true;
        if (activeGoal.goalType.$name === 'DISTANCE_GOAL') {
            $scope.validateDuration($scope.duration);
        }
        if ($scope.activityForm.$valid && !$scope.activityForm.durationHours.$invalid) {
            $scope.successfulSubmission = true;
            $scope.buttonText = "Logging your run..";
            $scope.activity.duration = toAppSeconds($scope.duration);
            $scope.activity.activityDate = moment($scope.activity.activityDate.toLocaleString(), "MM/DD/yyyy").format("YYYY-MM-DD");
            ActivityService.postActivity($scope.activity, activeGoal.id).success(function (data, status, headers, config) {
                $rootScope.$broadcast('update.progress', 'true');
                toastr.success("Posted new activity");
                $location.path('/activity/' + data.id);
            }).error(function (data, status, headers, config) {
                console.log("Error handler for PostActivity. Status code " + status);
                toastr.error("Unable to post activity. Please try later.");
                $location.path('/');
            });
        }


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
        $scope.minDate = $scope.activeGoal.startDate ? new Date($scope.activeGoal.startDate.time) : null;
        var current = new Date();
        $scope.maxDate = $scope.activeGoal.endDate ? (new Date($scope.activeGoal.endDate.time) > current ? current : new Date($scope.activeGoal.endDate.time)) : current;

    };
    $scope.toggleMin();

    $scope.open = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();

        $scope.opened = true;
    };

    $scope.dateOptions = {
        'year-format': "'yy'",
        'starting-day': 1
    };

    $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'shortDate'];
    $scope.format = $scope.formats[0];
}

function toAppSeconds(duration) {
    if (duration) {
        var hours = duration.hours && duration.hours !== '00' ? duration.hours : 0;
        var minutes = duration.minutes && duration.minutes !== '00' ? duration.minutes : 0;
        var seconds = duration.seconds && duration.seconds !== '00' ? duration.seconds : 0;
        return Number(hours) * 60 * 60 + Number(minutes) * 60 + Number(seconds);
    }
    return 0;
}
angular.module('milestogo')
    .controller('PostActivityCtrl', PostActivityCtrl);
