package org.miles2run.jaxrs.api.v1;

import org.apache.commons.lang3.StringUtils;
import org.jug.filters.LoggedIn;
import org.miles2run.business.domain.jpa.*;
import org.miles2run.business.services.jpa.ActivityJPAService;
import org.miles2run.business.services.jpa.GoalJPAService;
import org.miles2run.business.services.jpa.ProfileService;
import org.miles2run.business.services.redis.CommunityRunRedisService;
import org.miles2run.business.services.redis.CounterService;
import org.miles2run.business.services.redis.GoalRedisService;
import org.miles2run.business.services.redis.TimelineService;
import org.miles2run.business.services.social.FacebookService;
import org.miles2run.business.services.social.GoogleService;
import org.miles2run.business.services.social.TwitterService;
import org.miles2run.business.utils.UrlUtils;
import org.miles2run.business.vo.ActivityDetails;
import org.miles2run.jaxrs.requests.ActivityRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Created by shekhargulati on 15/03/14.
 */
@Path("/api/v1/goals/{goalId}/activities")
public class ActivityResource {

    private Logger logger = LoggerFactory.getLogger(ActivityResource.class);

    @Inject
    private ActivityJPAService activityJPAService;
    @Inject
    private ProfileService profileService;
    @Inject
    private TwitterService twitterService;
    @Inject
    private FacebookService facebookService;
    @Inject
    private CounterService counterService;
    @Context
    private HttpServletRequest request;
    @Inject
    private GoogleService googleService;
    @Inject
    private TimelineService timelineService;
    @Context
    private SecurityContext securityContext;
    @Inject
    private GoalJPAService goalJPAService;
    @Inject
    private CommunityRunRedisService communityRunRedisService;
    @Inject
    private GoalRedisService goalRedisService;


    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @LoggedIn
    public Response postActivity(@PathParam("goalId") Long goalId, @Valid final ActivityRequest activityRequest) {
        logger.info("Posting Activity {}", activityRequest);
        Activity activity = activityRequest.toActivity();
        String loggedInUser = securityContext.getUserPrincipal().getName();
        Profile profile = profileService.findProfile(loggedInUser);
        Goal goal = goalJPAService.findGoal(profile, goalId);
        logger.debug("Found goal {}", goal);
        if (goal == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No goal exists with id " + goalId).build();
        }
        activity.setPostedBy(profile);
        activity.setGoal(goal);
        Long persistedActivityId = activityJPAService.save(activity);
        ActivityDetails savedActivity = activityJPAService.findById(persistedActivityId);
        counterService.updateDistanceCount(activity.getDistanceCovered());
        counterService.updateActivitySecondsCount(activity.getDuration());
        goalRedisService.updateTotalDistanceCoveredForAGoal(goal.getId(), savedActivity.getDistanceCovered());
        timelineService.postActivityToTimeline(savedActivity, profile, goal);
        if (goal.getGoalType() == GoalType.COMMUNITY_RUN_GOAL) {
            communityRunRedisService.updateCommunityRunStats(loggedInUser, goal, activity);
        }
        return Response.status(Response.Status.CREATED).entity(ActivityDetails.toHumanReadable(savedActivity)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public ActivityDetails get(@NotNull @PathParam("id") Long id) {
        return ActivityDetails.toHumanReadable(activityJPAService.findById(id));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @LoggedIn
    public Response updateActivity(@PathParam("goalId") Long goalId, @PathParam("id") Long id, @Valid Activity activity) {
        String loggedInUser = securityContext.getUserPrincipal().getName();
        Profile profile = profileService.findProfile(loggedInUser);
        Goal goal = goalJPAService.findGoal(profile, goalId);
        if (goal == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No goal exists with id " + goalId).build();
        }

        ActivityDetails existingActivity = activityJPAService.findById(id);
        if (existingActivity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String activityBy = existingActivity.getUsername();
        if (!StringUtils.equals(loggedInUser, activityBy)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        double distanceCovered = activity.getDistanceCovered() * activity.getGoalUnit().getConversion();
        activity.setDistanceCovered(distanceCovered);
        ActivityDetails updatedActivity = activityJPAService.update(existingActivity, activity);
        timelineService.updateActivity(updatedActivity, profile, goal);
        updateStats(goal, existingActivity.getDistanceCovered(), existingActivity.getDuration(), activity.getDistanceCovered(), activity.getDuration());
        return Response.status(Response.Status.OK).entity(ActivityDetails.toHumanReadable(updatedActivity)).build();
    }

    @DELETE
    @Path("/{activityId}")
    @LoggedIn
    public Response deleteActivity(@PathParam("goalId") Long goalId, @PathParam("activityId") Long activityId) {
        String loggedInUser = securityContext.getUserPrincipal().getName();
        Profile profile = profileService.findProfile(loggedInUser);
        Goal goal = goalJPAService.findGoal(profile, goalId);
        if (goal == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No goal exists with id " + goalId).build();
        }
        ActivityDetails existingActivity = activityJPAService.findById(activityId);
        if (existingActivity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String activityBy = existingActivity.getUsername();
        if (!StringUtils.equals(loggedInUser, activityBy)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        activityJPAService.delete(activityId);
        timelineService.deleteActivityFromTimeline(loggedInUser, activityId, goal);
        updateStats(goal, existingActivity.getDistanceCovered(), existingActivity.getDuration(), 0.0d, 0L);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/share")
    @LoggedIn
    public Response shareActivity(@PathParam("id") Long id, Activity activity) {
        String loggedInUser = securityContext.getUserPrincipal().getName();
        Profile profile = profileService.findProfile(loggedInUser);
        Share share = activity.getShare();
        shareActivity(toActivityMessage(activity, profile), profile, share);
        return Response.ok().build();
    }

    private String toActivityMessage(Activity activity, Profile profile) {
        String activityUrl = UrlUtils.absoluteUrlForResourceUri(request, "/activities/{activityId}", profile.getUsername(), activity.getId());
        return new StringBuilder(profile.getFullname()).append(" ran ").append(activity.getDistanceCovered() / activity.getGoalUnit().getConversion()).append(" " + activity.getGoalUnit().toString()).append(" via @miles2runorg.").append(" Read full status here ").append(activityUrl).toString();
    }


    private void shareActivity(String message, Profile profile, Share share) {
        logger.info("in shareActivity() .. " + share);
        if (share != null) {

            for (SocialConnection socialConnection : profile.getSocialConnections()) {
                if (share.isTwitter() && socialConnection.getProvider() == SocialProvider.TWITTER) {
                    logger.info(String.format("Tweeting message : %s", message));
                    twitterService.postStatus(message, socialConnection);
                }
                if (share.isFacebook() && socialConnection.getProvider() == SocialProvider.FACEBOOK) {
                    logger.info(String.format("Posting message on Facebook wall : %s", message));
                    facebookService.postStatus(message, socialConnection);
                }

                if (share.isGooglePlus() && socialConnection.getProvider() == SocialProvider.GOOGLE_PLUS) {
                    logger.info(String.format("Posting message on G+ : %s", message));
                    googleService.postStatus(message, socialConnection);
                }
            }

        }
    }

    void updateStats(Goal goal, double existingDistanceCovered, long existingDuration, double distanceCovered, long duration) {
        Long goalId = goal.getId();
        double updatedDistanceCovered = distanceCovered - existingDistanceCovered;
        counterService.updateDistanceCount(updatedDistanceCovered);
        long updatedDuration = duration - existingDuration;
        counterService.updateActivitySecondsCount(updatedDuration);
        goalRedisService.updateTotalDistanceCoveredForAGoal(goalId, updatedDistanceCovered);
        if (goal.getGoalType() == GoalType.COMMUNITY_RUN_GOAL) {
            communityRunRedisService.updateCommunityRunDistanceAndDurationStats(goal.getCommunityRun().getSlug(), updatedDistanceCovered, updatedDuration);
        }
    }
}
