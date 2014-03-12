/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.tests.stress;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import org.osiam.client.connector.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.base.Strings;

public class RequesterJob implements Job {

    private String jobName;
    private OsiamConnector osiamConnector;
    private AccessToken accessToken;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        jobName = context.getJobDetail().getKey().getName();
        osiamConnector = OsiamContext.getInstance().getConnector(jobName);
        accessToken = OsiamContext.getInstance().getValidAccessToken();

        try {
            int i = (int) (Math.random() * 30 + 1);

            if (i > 0 && i <= 10) {
                createNewUser();
            } else if (i > 10 && i <= 18) {
                searchUser();
            } else if (i > 18 && i <= 26) {
                getUser();
            } else if (i > 26 && i <= 28) {
                updateUser();
            } else if (i > 28 && i <= 29) {
                replaceUser();
            } else if (i == 30) {
                deleteUser();
            }

        } catch (Throwable e) {
            System.out.println("Error at Job " + jobName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createNewUser() {
        logMessage("Creating a new User");

        User user = RandomUser.getNewUser();
        osiamConnector.createUser(user, accessToken);
    }

    private int getRandomNumber() {
        return (int) (Math.random() * 99 + 1);
    }

    private void updateUser() {
        logMessage("Updating a User");
        String userId = OsiamContext.getInstance().retrieveSingleUserId();
        logMessage("try to update user with the id " + userId);
        if (!Strings.isNullOrEmpty(userId)) {
            UpdateUser updateUser = new UpdateUser.Builder().updateExternalId(UUID.randomUUID().toString()).build();
            osiamConnector.updateUser(userId, updateUser, accessToken);
        }
    }

    private void replaceUser() {
        logMessage("Replace a User");
        String userId = OsiamContext.getInstance().retrieveSingleUserId();
        logMessage("try to replace user with the id " + userId);
        if (!Strings.isNullOrEmpty(userId)) {
            User replaceUser = RandomUser.getNewUser();
            osiamConnector.replaceUser(userId, replaceUser, accessToken);
        }
    }

    private void deleteUser() {
        logMessage("deleting a User");
        String userId = OsiamContext.getInstance().retrieveSingleUserId();
        logMessage("try to delete user with the id " + userId);
        if (!Strings.isNullOrEmpty(userId)) {
            osiamConnector.deleteUser(userId, accessToken);
        }
    }

    private void searchUser() throws UnsupportedEncodingException {
        logMessage("searching for Users");
        String query = getCompletUserQueryString();
        SCIMSearchResult<User> queryResult = osiamConnector.searchUsers("filter=" + query,
                accessToken);
        if (queryResult.getTotalResults() == 0) {
            queryResult = osiamConnector.searchUsers("filter=",
                    accessToken);
        }
        logMessage("found " + queryResult.getResources().size() + " users");
        OsiamContext.getInstance().setListOfUsers(queryResult.getResources());
    }

    private String getCompletUserQueryString() throws UnsupportedEncodingException {
        String encoded = null;
        encoded = URLEncoder.encode("active eq \"true\""
                + " and addresses.postalCode co \"" + getRandomNumber() + "\""
                + " and addresses.primary eq \"true\"", "UTF-8");
        return encoded;
    }

    private void getUser() {
        logMessage("get a Users");
        String userId = OsiamContext.getInstance().retrieveSingleUserId();
        logMessage("try to getuser with id: " + userId);
        if (!Strings.isNullOrEmpty(userId)) {
            osiamConnector.getUser(userId, accessToken);
        }
    }

    private void logMessage(String message) {
        System.out.println(jobName + ": " + message);
    }

}