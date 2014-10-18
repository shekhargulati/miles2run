package org.miles2run.business.services.jpa;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.miles2run.business.domain.jpa.*;
import org.miles2run.business.producers.EntityManagerProducer;
import org.miles2run.business.vo.ProfileDetails;
import org.miles2run.business.vo.ProfileGroupDetails;
import org.miles2run.business.vo.ProfileSocialConnectionDetails;
import org.miles2run.jaxrs.forms.ProfileForm;
import org.miles2run.shared.repositories.ProfileRepository;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by shekhargulati on 02/08/14.
 */
@RunWith(Arquillian.class)
public class CommunityRunJoinTest {

    @Inject
    private CommunityRunJPAService communityRunJPAService;
    @Inject
    private EntityManager entityManager;
    @Inject
    private UserTransaction userTransaction;
    @Inject
    private ProfileRepository profileRepository;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class).
                addClass(CommunityRun.class).
                addClass(BaseEntity.class).
                addClass(Profile.class).
                addClass(SocialConnection.class).
                addClass(ProfileRepository.class).
                addClass(ProfileDetails.class).
                addClass(ProfileSocialConnectionDetails.class).
                addClass(SocialProvider.class).
                addClass(Role.class).
                addClass(Gender.class).
                addClass(ProfileForm.class).
                addClass(ProfileGroupDetails.class).
                addClass(CommunityRunBuilder.class).
                addClass(CommunityRunJPAService.class).
                addClass(EntityManagerProducer.class).
                addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("joda-time:joda-time").withoutTransitivity().asFile()).
                addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.jadira.usertype:usertype.core").withTransitivity().asFile()).
                addAsResource("META-INF/test_persistence.xml", "META-INF/persistence.xml").
                addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        System.out.printf("WebArchive %s", webArchive.toString(true));
        return webArchive;
    }

    @Before
    public void setUp() throws Exception {
        userTransaction.begin();
        entityManager.createQuery("DELETE from CommunityRun cr").executeUpdate();
        entityManager.createQuery("DELETE from Profile p").executeUpdate();
        userTransaction.commit();
    }

    @Test
    public void shouldGroupAllUsersParticipatingInACommunityRun() throws Exception {
        // create 20 profiles which are not part of Community Run
        List<Profile> profiles = createProfiles(20);
        List<Profile> profilesPartOfCR = profiles.subList(0, 8);
        // Create a new CR and add only 8 profiles to it
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", "javaone-2014");
        communityRun.getProfiles().addAll(profilesPartOfCR);
        Long communityRunId = communityRunJPAService.save(communityRun);
        Assert.assertNotNull(communityRunId);

        // should find 4 groups that are part of this community run
        List<ProfileGroupDetails> usersByCity = communityRunJPAService.groupAllUserInACommunityRunByCity("javaone-2014");
        Assert.assertEquals(4, usersByCity.size());

        for (ProfileGroupDetails profileGroupDetails : usersByCity) {
            Assert.assertEquals(2, profileGroupDetails.getCount());
        }
    }

    private List<Profile> createProfiles(int n) {
        List<Profile> profiles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int cityId = i % 4;
            Profile profile = Profile.createProfile("test@test.com" + i, "test_user" + i, "Test User", "city" + cityId, "country", Gender.MALE);
            profileRepository.save(profile);
            profiles.add(profile);
        }
        return profiles;
    }

    private CommunityRun createCommunityRun(String name, String slug) {
        return new CommunityRunBuilder().
                setName(name).
                setBannerImg("http://example.com/javaone.png").
                setDescription("biggest Java conference").
                setSlug(slug).
                setStartDate(new Date()).
                setEndDate(new DateTime().plusDays(5).toDate()).
                setTwitterHandle("javaoneconf").
                setWebsite("https://www.oracle.com/javaone/index.html").
                createCommunityRun();
    }

    @Test
    public void shouldGroupAllUsersParticipatingInACommunityRunWithBigTestData() throws Exception {
        // create 2000 profiles which are not part of Community Run
        List<Profile> profiles = createProfiles(2000);
        List<Profile> profilesPartOfCR = profiles.subList(0, 800);
        // Create a new CR and add only 500 profiles to it
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", "javaone-2014");
        communityRun.getProfiles().addAll(profilesPartOfCR);
        Long communityRunId = communityRunJPAService.save(communityRun);
        Assert.assertNotNull(communityRunId);

        // should find 4 groups that are part of this community run
        List<ProfileGroupDetails> usersByCity = communityRunJPAService.groupAllUserInACommunityRunByCity("javaone-2014");
        Assert.assertEquals(4, usersByCity.size());

        for (ProfileGroupDetails profileGroupDetails : usersByCity) {
            Assert.assertEquals(200, profileGroupDetails.getCount());
        }
    }

    @Test
    public void shouldReturnEmptyListWhenNoOneHasJoinedACommunityRun() throws Exception {

        CommunityRun communityRun = createCommunityRun("JavaOne 2014", "javaone-2014");
        Long communityRunId = communityRunJPAService.save(communityRun);
        Assert.assertNotNull(communityRunId);

        // should find 4 groups that are part of this community run
        List<ProfileGroupDetails> usersByCity = communityRunJPAService.groupAllUserInACommunityRunByCity("javaone-2014");
        Assert.assertEquals(0, usersByCity.size());
    }

    @Test
    public void shouldAddProfileToACommunityRun() throws Exception {
        Profile profile = createProfiles(1).get(0);
        String slug = "javaone-2014";
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", slug);
        Long communityRunId = communityRunJPAService.save(communityRun);
        communityRunJPAService.addRunnerToCommunityRun(slug, profile);
        Assert.assertEquals(1, communityRunJPAService.findAllRunners(slug).size());
    }

    @Test
    public void userShouldBeAllowedToJoinMultipleCommunityRuns() throws Exception {
        Profile profile = createProfiles(1).get(0);
        String slug1 = "javaone-2014";
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", slug1);
        Long communityRunId = communityRunJPAService.save(communityRun);
        communityRunJPAService.addRunnerToCommunityRun(slug1, profile);
        Assert.assertEquals(1, communityRunJPAService.findAllRunners(slug1).size());

        String slug2 = "javaone-2015";
        CommunityRun anotherCommunityRun = createCommunityRun("JavaOne 2015", slug2);
        Long anotherCommunityRunId = communityRunJPAService.save(anotherCommunityRun);
        communityRunJPAService.addRunnerToCommunityRun(slug2, profile);
        Assert.assertEquals(1, communityRunJPAService.findAllRunners(slug2).size());

    }

    @Test
    public void usersShouldBeAllowedToConcurrentlyJoinCommunityRun() throws Exception {
        final AtomicInteger atomicInteger = new AtomicInteger(-1);
        final List<Profile> profiles = createProfiles(10);
        final String slug = "javaone-2014";
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", slug);
        final Long communityRunId = communityRunJPAService.save(communityRun);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    communityRunJPAService.addRunnerToCommunityRun(slug, profiles.get(atomicInteger.incrementAndGet()));
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        List<Profile> runners = communityRunJPAService.findAllRunners(slug);
        Assert.assertEquals(10, runners.size());
    }

    @Test
    public void userShouldBeAllowedToLeaveACommunityRun() throws Exception {
        Profile profile = createProfiles(1).get(0);
        String slug = "javaone-2014";
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", slug);
        final Long communityRunId = communityRunJPAService.save(communityRun);
        communityRunJPAService.addRunnerToCommunityRun(slug, profile);

        communityRunJPAService.leaveCommunityRun(slug, profile);
        List<Profile> runners = communityRunJPAService.findAllRunners(slug);
        Assert.assertEquals(0, runners.size());

    }

    @Test
    public void usersShouldBeAllowedToConcurrentlyLeaveACommunityRun() throws Exception {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final List<Profile> profiles = createProfiles(10);
        final String slug = "javaone-2014";
        CommunityRun communityRun = createCommunityRun("JavaOne 2014", slug);
        communityRun.getProfiles().addAll(profiles);
        final Long communityRunId = communityRunJPAService.save(communityRun);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Profile profile = profiles.get(atomicInteger.getAndIncrement());
                    System.out.printf("Leaving community run %s", profile.toString());
                    communityRunJPAService.leaveCommunityRun(slug, profile);
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        List<Profile> runners = communityRunJPAService.findAllRunners(slug);
        Assert.assertEquals(0, runners.size());
    }
}
