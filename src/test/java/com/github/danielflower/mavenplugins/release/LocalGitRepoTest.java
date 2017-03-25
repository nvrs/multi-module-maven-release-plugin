package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Test;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.TestProject.dirToGitScmReference;
import static com.github.danielflower.mavenplugins.release.GitHelper.scmUrlToRemote;

public class LocalGitRepoTest {

    TestProject project = TestProject.singleModuleProject();

    @Test
    public void canDetectLocalTags() throws GitAPIException {
        LocalGitRepo repo = new LocalGitRepo(project.local, null);
        tag(project.local, "some-tag");
        assertThat(repo.hasLocalTag("some-tag"), is(true));
        assertThat(repo.hasLocalTag("some-ta"), is(false));
        assertThat(repo.hasLocalTag("some-tagyo"), is(false));
    }

    @Test
    public void canDetectRemoteTags() throws Exception {
        LocalGitRepo repo = new LocalGitRepo(project.local, null);
        tag(project.origin, "some-tag");
        assertThat(repo.remoteTagsFrom(tags("blah", "some-tag")), equalTo(asList("some-tag")));
        assertThat(repo.remoteTagsFrom(tags("blah", "some-taggart")), equalTo(emptyList()));
    }

    @Test
    public void usesThePassedInScmUrlToFindRemote() throws Exception {
        LocalGitRepo repo = new LocalGitRepo(project.local, scmUrlToRemote(dirToGitScmReference(project.originDir)));
        tag(project.origin, "some-tag");

        StoredConfig config = project.local.getRepository().getConfig();
        config.unsetSection("remote", "origin");
        config.save();

        assertThat(repo.remoteTagsFrom(tags("blah", "some-tag")), equalTo(asList("some-tag")));
    }

    @Test
    public void canHaveManyTags() throws GitAPIException {
        int numberOfTags = 50; // setting this to 1000 works but takes too long
        for (int i = 0; i < numberOfTags; i++) {
            tag(project.local, "this-is-a-tag-" + i);
        }
        project.local.push().setPushTags().call();
        LocalGitRepo repo = new LocalGitRepo(project.local, null);
        for (int i = 0; i < numberOfTags; i++) {
            String tagName = "this-is-a-tag-" + i;
            assertThat(repo.hasLocalTag(tagName), is(true));
            assertThat(repo.remoteTagsFrom(tags(tagName)).size(), is(1));
        }
    }

    @Test
    public void canDeleteLocalTag() throws GitAPIException, IOException {
        TestProject project = TestProject.singleModuleProject();
        LocalGitRepo repo = new LocalGitRepo(project.local, null);

        String tagName = "some-tag-to-delete";
        tag(project.local, tagName);
        assertThat(repo.hasLocalTag(tagName), is(true));

        repo.deleteTag(tagName);
        assertThat(repo.hasLocalTag(tagName), is(false));
    }

    @Test
    public void canDeleteLocalAndRemoteTag() throws GitAPIException, IOException, ValidationException {
        TestProject project = TestProject.singleModuleProject();
        AnnotatedTag tag = AnnotatedTag.create("my-name", "the-version", 2134);
        LocalGitRepo repo = new LocalGitRepo(project.local, scmUrlToRemote(dirToGitScmReference(project.originDir)));

        repo.tagRepoAndPush(tag);
        assertThat(repo.hasLocalTag(tag.name()), is(true));
        assertThat(repo.remoteTagsFrom(tags(tag.name())).size(), is(1));

        repo.deleteTag(tag, true);
        assertThat(repo.hasLocalTag(tag.name()), is(false));
        assertThat(repo.remoteTagsFrom(tags(tag.name())).size(), is(0));

    }

    private static List<AnnotatedTag> tags(String... tagNames) {
        List<AnnotatedTag> tags = new ArrayList<AnnotatedTag>();
        for (String tagName : tagNames) {
            tags.add(AnnotatedTag.create(tagName, "1", 0));
        }
        return tags;
    }
    private static List<String> emptyList() {
        return new ArrayList<String>();
    }

    private static void tag(Git repo, String name) throws GitAPIException {
        repo.tag().setAnnotated(true).setName(name).setMessage("Some message").call();
    }
}
