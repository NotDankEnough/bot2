use std::{
    collections::{HashMap, HashSet},
    env,
    sync::Arc,
    time::Duration,
};

use common::{
    establish_connection,
    models::{Channel, ChannelFeature, ChannelPreference, EventFlag, EventType},
};
use diesel::{BelongingToDsl, ExpressionMethods, QueryDsl, RunQueryDsl};
use log::{info, warn};
use reqwest::StatusCode;
use serde::Deserialize;
use twitch_api::helix::chat::GetChattersRequest;

use crate::{instance_bundle::InstanceBundle, utils::split_and_wrap_lines};
use common::{
    models::{EventSubscription, User},
    schema::{
        channels::dsl as ch, event_subscriptions::dsl as evs, events::dsl as ev, users::dsl as us,
    },
};

#[derive(Deserialize)]
struct GithubResponse {
    pub commit: GithubCommit,
}

#[derive(Deserialize)]
struct GithubCommitter {
    pub name: String,
}

#[derive(Deserialize)]
struct GithubTree {
    pub sha: String,
}

#[derive(Deserialize)]
struct GithubCommit {
    pub committer: GithubCommitter,
    pub message: String,
    pub tree: GithubTree,
}

pub struct GithubCommitsHelper {
    bundle: Arc<InstanceBundle>,
    commits_cache: HashMap<String, Vec<String>>,
}

impl GithubCommitsHelper {
    pub fn new(bundle: Arc<InstanceBundle>) -> Self {
        Self {
            bundle,
            commits_cache: HashMap::new(),
        }
    }

    pub async fn run(&mut self) {
        info!("Starting to listen to GitHub commits...");

        if env::var("GITHUB_API_TOKEN").is_err() {
            log::warn!("GITHUB_API_TOKEN is not set, so GitHub events will not be listened to until a restart with the value set.");
            return;
        }

        loop {
            self.process_repos().await;
            tokio::time::sleep(Duration::from_secs(60)).await;
        }
    }

    #[allow(clippy::type_complexity)]
    async fn process_repos(&mut self) {
        let conn = &mut establish_connection();
        let events: Vec<(i32, Vec<EventFlag>, i32, Option<String>, String)> = ev::events
            .filter(ev::event_type.eq(&EventType::Github))
            .select((
                ev::id,
                ev::flags,
                ev::channel_id,
                ev::custom_alias_id,
                ev::message,
            ))
            .get_results::<(i32, Vec<EventFlag>, i32, Option<String>, String)>(conn)
            .unwrap_or(Vec::new());
        let reqwest_client = reqwest::Client::new();
        let github_token = env::var("GITHUB_API_TOKEN").unwrap();
        let mut map: HashMap<String, Vec<GithubResponse>> = HashMap::new();

        // getting commits
        for (_, _, _, name, _) in &events {
            if let Some(name) = name {
                if map.contains_key(name) {
                    continue;
                }

                let url = format!("https://api.github.com/repos/{}/commits", name);

                let response = reqwest_client
                    .get(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", format!("Bearer {}", github_token))
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header(
                        "User-Agent",
                        format!(
                            "{} - Twitch bot - https://github.com/ilotterytea/bot",
                            env::var("BOT_USERNAME").unwrap()
                        ),
                    )
                    .send()
                    .await;

                let json = match response {
                    Ok(response) => {
                        if response.status() == StatusCode::OK {
                            response.json::<Vec<GithubResponse>>().await
                        } else {
                            warn!("GitHub REST API returned {}", response.status());
                            continue;
                        }
                    }
                    Err(e) => {
                        warn!("Failed to send GitHub commit response: {}", e);
                        continue;
                    }
                };

                match json {
                    Ok(json) => map.insert(name.clone(), json),
                    Err(e) => {
                        warn!("Failed to deserialize GitHub commit response: {}", e);
                        continue;
                    }
                };
            }
        }

        // filter commits
        let mut new_map: HashMap<&String, Vec<&GithubResponse>> = HashMap::new();

        for (k, v) in map.iter() {
            let mut commits: Vec<&GithubResponse> = Vec::new();
            if let Some(old_shas) = self.commits_cache.get(k) {
                for resp in v.iter() {
                    if !old_shas.contains(&resp.commit.tree.sha) {
                        commits.push(resp);
                    }
                }
            } else {
                self.commits_cache.insert(
                    k.clone(),
                    v.iter()
                        .map(|x| x.commit.tree.sha.clone())
                        .collect::<Vec<String>>(),
                );
                continue;
            }
            new_map.insert(k, commits);
        }

        // sending notifications
        for (eid, eflags, cid, name, message) in events {
            if let Some(name) = name {
                if let Some(commits) = new_map.get(&name) {
                    let channel = ch::channels
                        .filter(ch::id.eq(&cid))
                        .filter(ch::opt_outed_at.is_null())
                        .get_result::<Channel>(conn);

                    if channel.is_err() {
                        warn!("GitHub commit notification wasn't send because the alias name (ID {}) is empty!", cid);
                        continue;
                    }

                    let channel: Channel = channel.unwrap();

                    let preferences: ChannelPreference = ChannelPreference::belonging_to(&channel)
                        .first::<ChannelPreference>(conn)
                        .expect("Failed to load preferences");

                    let features: Vec<&String> = preferences.features.iter().flatten().collect();
                    if features.contains(&&ChannelFeature::ShutupChannel.to_string()) {
                        continue;
                    }

                    let subs = match evs::event_subscriptions
                        .filter(evs::event_id.eq(&eid))
                        .get_results::<EventSubscription>(conn)
                    {
                        Ok(v) => v,
                        Err(e) => {
                            log::error!("Failed to get subscriptions for event ID {}: {}", eid, e);
                            return;
                        }
                    };

                    let users = match us::users.load::<User>(conn) {
                        Ok(v) => v,
                        Err(e) => {
                            log::error!("Failed to get users for event ID {}: {}", eid, e);
                            return;
                        }
                    };

                    let users = users
                        .iter()
                        .filter(|x| subs.iter().any(|y| y.user_id == x.id))
                        .map(|x| format!("@{}", x.alias_name))
                        .collect::<Vec<String>>();

                    let mut subs: HashSet<String> = HashSet::new();

                    subs.extend(users);

                    if eflags.contains(&EventFlag::Massping) {
                        let broadcaster_id = channel.alias_id.to_string();
                        let moderator_id = self.bundle.twitch_api_token.user_id.clone().take();

                        let chatters = match self
                            .bundle
                            .twitch_api_client
                            .req_get(
                                GetChattersRequest::new(
                                    broadcaster_id.as_str(),
                                    moderator_id.as_str(),
                                ),
                                &*self.bundle.twitch_api_token,
                            )
                            .await
                        {
                            Ok(v) => v,
                            Err(e) => {
                                log::error!("Failed to get chatters for event ID {}: {}", eid, e);
                                return;
                            }
                        };

                        let chatters = chatters
                            .data
                            .iter()
                            .map(|x| format!("@{}", x.user_login))
                            .collect::<HashSet<String>>();

                        subs.extend(chatters);
                    }

                    let placeholders = self.bundle.localizator.parse_placeholders(&message);

                    for commit in commits {
                        if let Some(old_shas) = self.commits_cache.get_mut(&name) {
                            old_shas.push(commit.commit.tree.sha.clone());
                        }

                        let line = self.bundle.localizator.replace_placeholders(
                            message.clone(),
                            placeholders.clone(),
                            vec![
                                name.clone(),
                                commit.commit.committer.name.clone(),
                                commit.commit.tree.sha.clone(),
                                commit.commit.message.clone(),
                            ],
                            None,
                        );

                        if subs.is_empty() {
                            self.bundle
                                .twitch_irc_client
                                .say(channel.alias_name.clone(), format!("🧑‍💻 {}", line))
                                .await
                                .expect("Failed to send a message");
                            return;
                        }

                        let formatted_subs = split_and_wrap_lines(
                            subs.clone()
                                .into_iter()
                                .collect::<Vec<String>>()
                                .join(", ")
                                .as_str(),
                            ", ",
                            300 - line.len(),
                        );

                        for formatted_sub in formatted_subs {
                            self.bundle
                                .twitch_irc_client
                                .say(
                                    channel.alias_name.clone(),
                                    format!("🧑‍💻 {} · {}", line, formatted_sub),
                                )
                                .await
                                .expect("Failed to send a message");
                        }
                    }
                }
            }
        }
    }
}
