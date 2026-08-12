package x.Entt.ClansX.Utils;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

import x.Entt.ClansX.CX;

import static x.Entt.ClansX.CX.prefix;

public class MSG {
    private static CX plugin;

    public enum Paths {
        HINT("general.hint"),

        CONSOLE("errors.console"),
        MAX_WARS("errors.max_wars"),
        NO_PERMS("errors.no_perms"),
        IN_TRUCE("errors.in_truce"),
        NOT_LEADER("errors.not_leader"),
        SHOULD_USE("errors.should_use"),
        NOT_IN_WAR("errors.not_in_war"),
        NOT_IN_CLAN("errors.not_in_clan"),
        INVALID_SIDE("errors.invalid_side"),
        TOO_MANY_WARS("errors.too_many_wars"),
        WAR_NOT_FOUND("errors.war_not_found"),
        CLAN_NOT_FOUND("errors.clan_not_found"),
        NO_BALANCE_WAR("errors.no_balance_war"),
        ALREADY_AT_WAR("errors.already_at_war"),
        ALREADY_IN_CLAN("errors.already_in_clan"),
        UNEXISTENT_CLAN("errors.unexistent_clan"),
        SELF_DECLARATION("errors.self_declaration"),
        TERM_NOT_ENABLED("errors.term_not_enabled"),
        AMOUNT_NOT_NUMBER("errors.amount_not_number"),
        BELOW_MIN_BALANCE("errors.below_min_balance"),
        ALREADY_JOINED_WAR("errors.already_joined_war"),
        INVALID_TERM_FORMAT("errors.invalid_term_format"),
        INFLUENCES_DISABLED("errors.influences_disabled"),
        NOT_ENOUGH_PRESTIGE("errors.not_enough_prestige"),
        PEACE_WINDOW_EXPIRED("errors.peace_window_expired"),
        TERM_REQUIRES_AMOUNT("errors.term_requires_amount"),
        WHITE_PEACE_COMBINED("errors.white_peace_combined"),
        PEACE_OFFER_NOT_FOUND("errors.peace_offer_not_found"),
        PEACE_OFFER_NOT_PENDING("errors.peace_offer_not_pending"),
        CANNOT_ACCEPT_OWN_OFFER("errors.cannot_accept_own_offer"),
        CANNOT_REJECT_OWN_OFFER("errors.cannot_reject_own_offer"),
        MAIN_CLANS_CANNOT_LEAVE("errors.main_clans_cannot_leave"),
        CLAN_DATA_CORRUPTED("errors.clan_data_corrupted"),
        PLAYER_NOT_FOUND("errors.player_not_found"),
        INVALID_ACTION("errors.invalid_action"),
        INVALID_AMOUNT("errors.invalid_amount"),
        OFFER_ID_NOT_NUMBER("errors.offer_id_not_number"),
        CANNOT_EXPEL_SELF("errors.cannot_expel_self"),
        PLAYER_NOT_IN_CLAN("errors.player_not_in_clan"),
        INVALID_CLAN_NAME("errors.invalid_clan_name"),
        SAME_NAME("errors.same_name"),
        CLAN_NAME_TAKEN("errors.clan_name_taken"),
        RENAME_FAILED("errors.rename_failed"),
        INVALID_PRIVACY_VALUE("errors.invalid_privacy_value"),
        INVALID_EDIT_OPTION("errors.invalid_edit_option"),
        AMOUNT_MUST_BE_POSITIVE("errors.amount_must_be_positive"),
        NOT_ENOUGH_MONEY("errors.not_enough_money"),
        CLAN_NOT_ENOUGH_MONEY("errors.clan_not_enough_money"),
        INVALID_OPERATION("errors.invalid_operation"),
        CANNOT_INVITE_SELF("errors.cannot_invite_self"),
        PLAYER_ALREADY_IN_CLAN("errors.player_already_in_clan"),
        CLAN_IS_PRIVATE("errors.clan_is_private"),
        NO_CLANS_REGISTERED("errors.no_clans_registered"),
        INVALID_REASON("errors.invalid_reason"),
        REPORT_ALREADY_SUBMITTED("errors.report_already_submitted"),
        MAX_CLANS_REACHED("errors.max_clans_reached"),
        NAME_NOT_ALLOWED("errors.name_not_allowed"),
        INSUFFICIENT_FUNDS("errors.insufficient_funds"),
        CLAN_CREATION_ERROR("errors.clan_creation_error"),

        WAR_ID("info.war_ID"),
        WAR_NAME("info.war_name"),
        WAR_NEWS("info.war_news"),
        RECEIVED_WAR("info.received_war"),
        WAR_DECLARED("info.war_declared"),
        RESIGN("info.resigned_as_leader"),
        CANNOT_RESIGN("info.cannot_resign"),
        WAR_SIDES("info.war_main_participants"),
        WAR_DECLARATION("info.war_declaration"),
        RECEIVED_WAR_INFO("info.received_war_info"),
        NEW_LEADER("info.new_leader"),
        NEW_LEADER_ADVICE("info.you_are_the_new_leader"),
        LOST_PERMS("info.perms_lost_in_clanChest"),
        JOINED_WAR("info.joined_war"),
        PEACE_OFFER_SENT("info.peace_offer_sent"),
        PEACE_OFFER_RECEIVED("info.peace_offer_received"),
        PEACE_OFFER_USE_ACCEPT("info.peace_offer_use_accept"),
        PEACE_ACCEPTED_BROADCAST("info.peace_accepted_broadcast"),
        PEACE_ACCEPTED_SELF("info.peace_accepted_self"),
        PEACE_OFFER_REJECTED("info.peace_offer_rejected"),
        PEACE_OFFER_REJECTED_NOTIFY("info.peace_offer_rejected_notify"),
        WAR_FORFEIT("info.war_forfeit"),
        WAR_LEFT("info.war_left"),
        PENDING_NOTIFICATIONS_HEADER("info.pending_notifications_header"),
        RELOAD_COMPLETE("info.reload_complete"),
        CLAN_DISBANDED("info.clan_disbanded"),
        CLAN_DISBANDED_REWARD("info.clan_disbanded_reward"),
        CLAN_DISBANDED_NOTIFY("info.clan_disbanded_notify"),
        CLAN_DISBANDED_NO_MEMBERS("info.clan_disbanded_no_members"),
        CLAN_DISBANDED_NO_MEMBERS_REWARD("info.clan_disbanded_no_members_reward"),
        NO_CLAN_REPORTS("info.no_clan_reports"),
        CLAN_ECONOMY_UPDATED("info.clan_economy_updated"),
        PLAYER_ECONOMY_UPDATED("info.player_economy_updated"),
        PLAYER_EXPELLED("info.player_expelled"),
        YOU_WERE_EXPELLED("info.you_were_expelled"),
        DEPOSITED("info.deposited"),
        WITHDREW("info.withdrew"),
        INVITATION_SENT("info.invitation_sent"),
        INVITED_TO_CLAN("info.invited_to_clan"),
        LEFT_CLAN_NEW_LEADER("info.left_clan_new_leader"),
        LEFT_CLAN("info.left_clan"),
        JOINED_CLAN("info.joined_clan"),
        LIST_HEADER("info.list_header"),
        LIST_FOOTER("info.list_footer"),
        LIST_ITEM_FORMAT("info.list_item_format"),
        CLAN_REPORTED("info.clan_reported"),
        CLAN_RENAMED("info.clan_renamed"),
        PRIVACY_UPDATED("info.privacy_updated"),
        CLAN_CREATED("info.clan_created"),

        INVITED_TO_CLANS("invitations.message"),
        INVITED_LIST_ITEM("invitations.list_item"),

        STATS_HEADER("stats.header"),
        STATS_NAME("stats.name"),
        STATS_FOUNDER("stats.founder"),
        STATS_LEADER("stats.leader"),
        STATS_PRIVACY("stats.privacy"),
        STATS_BALANCE("stats.balance"),
        STATS_MEMBERS("stats.members"),
        STATS_INVITED("stats.invited"),
        STATS_WARS("stats.wars"),
        STATS_ACTIVE_WARS("stats.active-wars"),
        STATS_WON_WARS("stats.won-wars"),
        STATS_DEF_WARS("stats.def-wars"),
        STATS_STREAK("stats.streak"),
        STATS_TOTAL_KILLS("stats.total-kills"),
        STATS_NONE("stats.none"),
        STATS_LIST_ITEM("stats.list-item"),
        STATS_FOOTER("stats.footer"),

        UI_PREVIOUS_PAGE("ui.previous"),
        UI_NEXT_PAGE("ui.next"),
        UI_BACK("ui.back"),

        WGUI_COMMON_BACK("gui.wars.common.back"),
        WGUI_NO_WAR_SELECTED("gui.wars.no_war_selected"),
        WGUI_MAIN_CANNOT_WITHDRAW("gui.wars.main_cannot_withdraw"),
        WGUI_CANNOT_ACCEPT_OWN("gui.wars.cannot_accept_own"),

        WGUI_MAIN_TITLE("gui.wars.main.title"),
        WGUI_MAIN_DECLARE_NAME("gui.wars.main.declare.name"),
        WGUI_MAIN_DECLARE_LORE("gui.wars.main.declare.lore"),
        WGUI_MAIN_DECLARE_CAN("gui.wars.main.declare.can"),
        WGUI_MAIN_DECLARE_CANNOT("gui.wars.main.declare.cannot"),
        WGUI_MAIN_MYWARS_NAME("gui.wars.main.mywars.name"),
        WGUI_MAIN_MYWARS_LORE("gui.wars.main.mywars.lore"),
        WGUI_MAIN_MYWARS_CAN("gui.wars.main.mywars.can"),
        WGUI_MAIN_MYWARS_CANNOT("gui.wars.main.mywars.cannot"),
        WGUI_MAIN_ALLWARS_NAME("gui.wars.main.allwars.name"),
        WGUI_MAIN_ALLWARS_LORE("gui.wars.main.allwars.lore"),
        WGUI_MAIN_JOIN_NAME("gui.wars.main.join.name"),
        WGUI_MAIN_JOIN_LORE("gui.wars.main.join.lore"),
        WGUI_MAIN_JOIN_CAN("gui.wars.main.join.can"),
        WGUI_MAIN_JOIN_CANNOT("gui.wars.main.join.cannot"),
        WGUI_MAIN_PEACE_NAME("gui.wars.main.peace.name"),
        WGUI_MAIN_PEACE_LORE("gui.wars.main.peace.lore"),
        WGUI_MAIN_PEACE_CAN("gui.wars.main.peace.can"),
        WGUI_MAIN_PEACE_CANNOT("gui.wars.main.peace.cannot"),
        WGUI_MAIN_OFFERS_NAME("gui.wars.main.offers.name"),
        WGUI_MAIN_OFFERS_LORE("gui.wars.main.offers.lore"),
        WGUI_MAIN_OFFERS_CAN("gui.wars.main.offers.can"),
        WGUI_MAIN_OFFERS_CANNOT("gui.wars.main.offers.cannot"),
        WGUI_MAIN_WITHDRAW_NAME("gui.wars.main.withdraw.name"),
        WGUI_MAIN_WITHDRAW_LORE("gui.wars.main.withdraw.lore"),
        WGUI_MAIN_WITHDRAW_CAN("gui.wars.main.withdraw.can"),
        WGUI_MAIN_WITHDRAW_CANNOT("gui.wars.main.withdraw.cannot"),
        WGUI_MAIN_STATS_NAME("gui.wars.main.stats.name"),
        WGUI_MAIN_STATS_LORE("gui.wars.main.stats.lore"),
        WGUI_MAIN_STATS_CAN("gui.wars.main.stats.can"),
        WGUI_MAIN_STATS_CANNOT("gui.wars.main.stats.cannot"),

        WGUI_DECLARE_TITLE("gui.wars.declare.title"),
        WGUI_DECLARE_TARGET_LORE("gui.wars.declare.target_lore"),
        WGUI_DECLARE_EMPTY_NAME("gui.wars.declare.empty_name"),
        WGUI_DECLARE_EMPTY_LORE("gui.wars.declare.empty_lore"),

        WGUI_MYWARS_TITLE("gui.wars.mywars.title"),
        WGUI_MYWARS_MANAGE_HINT("gui.wars.mywars.manage_hint"),
        WGUI_MYWARS_EMPTY_NAME("gui.wars.mywars.empty_name"),
        WGUI_MYWARS_EMPTY_LORE("gui.wars.mywars.empty_lore"),

        WGUI_JOIN_TITLE("gui.wars.join.title"),
        WGUI_JOIN_LEFT_CLICK("gui.wars.join.left_click"),
        WGUI_JOIN_RIGHT_CLICK("gui.wars.join.right_click"),
        WGUI_JOIN_EMPTY_NAME("gui.wars.join.empty_name"),
        WGUI_JOIN_EMPTY_LORE("gui.wars.join.empty_lore"),

        WGUI_PEACESELECT_TITLE("gui.wars.peaceselect.title"),
        WGUI_PEACESELECT_HINT("gui.wars.peaceselect.hint"),
        WGUI_PEACESELECT_EMPTY_NAME("gui.wars.peaceselect.empty_name"),
        WGUI_PEACESELECT_EMPTY_LORE("gui.wars.peaceselect.empty_lore"),

        WGUI_OFFERSSELECT_TITLE("gui.wars.offersselect.title"),
        WGUI_OFFERSSELECT_HAS_OFFERS("gui.wars.offersselect.has_offers"),
        WGUI_OFFERSSELECT_NO_OFFERS("gui.wars.offersselect.no_offers"),
        WGUI_OFFERSSELECT_EMPTY_NAME("gui.wars.offersselect.empty_name"),
        WGUI_OFFERSSELECT_EMPTY_LORE("gui.wars.offersselect.empty_lore"),

        WGUI_WITHDRAWSELECT_TITLE("gui.wars.withdrawselect.title"),
        WGUI_WITHDRAWSELECT_MAIN_CANNOT("gui.wars.withdrawselect.main_cannot"),
        WGUI_WITHDRAWSELECT_CAN_WITHDRAW("gui.wars.withdrawselect.can_withdraw"),
        WGUI_WITHDRAWSELECT_EMPTY_NAME("gui.wars.withdrawselect.empty_name"),
        WGUI_WITHDRAWSELECT_EMPTY_LORE("gui.wars.withdrawselect.empty_lore"),

        WGUI_STATS_TITLE("gui.wars.stats.title"),
        WGUI_STATS_KILLS_NAME("gui.wars.stats.kills_name"),
        WGUI_STATS_KILLS_LORE("gui.wars.stats.kills_lore"),
        WGUI_STATS_WON_NAME("gui.wars.stats.won_name"),
        WGUI_STATS_WON_LORE("gui.wars.stats.won_lore"),
        WGUI_STATS_DEFEATED_NAME("gui.wars.stats.defeated_name"),
        WGUI_STATS_DEFEATED_LORE("gui.wars.stats.defeated_lore"),
        WGUI_STATS_STREAK_NAME("gui.wars.stats.streak_name"),
        WGUI_STATS_STREAK_LORE("gui.wars.stats.streak_lore"),
        WGUI_STATS_ACTIVE_NAME("gui.wars.stats.active_name"),
        WGUI_STATS_ACTIVE_LORE("gui.wars.stats.active_lore"),

        WGUI_ALLWARS_TITLE("gui.wars.allwars.title"),
        WGUI_ALLWARS_EMPTY_NAME("gui.wars.allwars.empty_name"),
        WGUI_ALLWARS_EMPTY_LORE("gui.wars.allwars.empty_lore"),

        WGUI_DETAIL_INFO_NAME("gui.wars.detail.info_name"),
        WGUI_DETAIL_INFO_OFFENSIVE("gui.wars.detail.info_offensive"),
        WGUI_DETAIL_INFO_DEFENSIVE("gui.wars.detail.info_defensive"),
        WGUI_DETAIL_INFO_OFF_HEADER("gui.wars.detail.info_off_header"),
        WGUI_DETAIL_INFO_DEF_HEADER("gui.wars.detail.info_def_header"),
        WGUI_DETAIL_INFO_KILLS("gui.wars.detail.info_kills"),
        WGUI_DETAIL_INFO_DEATHS("gui.wars.detail.info_deaths"),
        WGUI_DETAIL_INFO_POINTS("gui.wars.detail.info_points"),
        WGUI_DETAIL_OFFER_FROM("gui.wars.detail.offer_from"),
        WGUI_DETAIL_OFFER_TERMS_HEADER("gui.wars.detail.offer_terms_header"),
        WGUI_DETAIL_OFFER_TERM_LINE("gui.wars.detail.offer_term_line"),
        WGUI_DETAIL_OFFER_ACCEPT("gui.wars.detail.offer_accept"),
        WGUI_DETAIL_OFFER_REJECT("gui.wars.detail.offer_reject"),
        WGUI_DETAIL_OFFER_NAME("gui.wars.detail.offer_name"),
        WGUI_DETAIL_PEACE_NAME("gui.wars.detail.peace_name"),
        WGUI_DETAIL_PEACE_LORE1("gui.wars.detail.peace_lore1"),
        WGUI_DETAIL_PEACE_LORE2("gui.wars.detail.peace_lore2"),
        WGUI_DETAIL_WITHDRAW_NAME("gui.wars.detail.withdraw_name"),
        WGUI_DETAIL_WITHDRAW_LORE("gui.wars.detail.withdraw_lore"),

        WGUI_PEACETYPE_TITLE("gui.wars.peacetype.title"),
        WGUI_PEACETYPE_SELECTED_NAME("gui.wars.peacetype.selected_name"),
        WGUI_PEACETYPE_SELECTED_HEADER("gui.wars.peacetype.selected_header"),
        WGUI_PEACETYPE_SELECTED_LINE("gui.wars.peacetype.selected_line"),
        WGUI_PEACETYPE_SELECTED_HINT("gui.wars.peacetype.selected_hint"),
        WGUI_PEACETYPE_WHITEPEACE_NAME("gui.wars.peacetype.whitepeace_name"),
        WGUI_PEACETYPE_WHITEPEACE_LORE("gui.wars.peacetype.whitepeace_lore"),
        WGUI_PEACETYPE_WHITEPEACE_MULTI("gui.wars.peacetype.whitepeace_multi"),
        WGUI_PEACETYPE_WHITEPEACE_CLICK("gui.wars.peacetype.whitepeace_click"),
        WGUI_PEACETYPE_MONEY_NAME("gui.wars.peacetype.money_name"),
        WGUI_PEACETYPE_MONEY_LORE1("gui.wars.peacetype.money_lore1"),
        WGUI_PEACETYPE_MONEY_LORE2("gui.wars.peacetype.money_lore2"),
        WGUI_PEACETYPE_MONEY_CLICK("gui.wars.peacetype.money_click"),
        WGUI_PEACETYPE_XP_NAME("gui.wars.peacetype.xp_name"),
        WGUI_PEACETYPE_XP_LORE1("gui.wars.peacetype.xp_lore1"),
        WGUI_PEACETYPE_XP_LORE2("gui.wars.peacetype.xp_lore2"),
        WGUI_PEACETYPE_XP_CLICK("gui.wars.peacetype.xp_click"),
        WGUI_PEACETYPE_DISBAND_NAME("gui.wars.peacetype.disband_name"),
        WGUI_PEACETYPE_DISBAND_LORE1("gui.wars.peacetype.disband_lore1"),
        WGUI_PEACETYPE_DISBAND_LORE2("gui.wars.peacetype.disband_lore2"),
        WGUI_PEACETYPE_DISBAND_CLICK("gui.wars.peacetype.disband_click"),
        WGUI_PEACETYPE_UNIFY_NAME("gui.wars.peacetype.unify_name"),
        WGUI_PEACETYPE_UNIFY_LORE1("gui.wars.peacetype.unify_lore1"),
        WGUI_PEACETYPE_UNIFY_LORE2("gui.wars.peacetype.unify_lore2"),
        WGUI_PEACETYPE_UNIFY_LORE3("gui.wars.peacetype.unify_lore3"),
        WGUI_PEACETYPE_UNIFY_CLICK("gui.wars.peacetype.unify_click"),
        WGUI_PEACETYPE_SEND_NAME("gui.wars.peacetype.send_name"),
        WGUI_PEACETYPE_SEND_LORE("gui.wars.peacetype.send_lore"),
        WGUI_PEACETYPE_CLEAR_NAME("gui.wars.peacetype.clear_name"),
        WGUI_PEACETYPE_CLEAR_LORE("gui.wars.peacetype.clear_lore"),
        WGUI_PEACETYPE_CLICK_SELECT("gui.wars.peacetype.click_select"),
        WGUI_PEACETYPE_CLICK_PROPOSE("gui.wars.peacetype.click_propose"),
        WGUI_PEACETYPE_CLICK_ADD("gui.wars.peacetype.click_add"),

        WGUI_MONEYINPUT_TITLE("gui.wars.moneyinput.title"),
        WGUI_MONEYINPUT_AMOUNT_NAME("gui.wars.moneyinput.amount_name"),
        WGUI_MONEYINPUT_AMOUNT_OPPONENT("gui.wars.moneyinput.amount_opponent"),
        WGUI_MONEYINPUT_AMOUNT_BALANCE("gui.wars.moneyinput.amount_balance"),
        WGUI_MONEYINPUT_AMOUNT_STEP("gui.wars.moneyinput.amount_step"),
        WGUI_MONEYINPUT_OPPONENT_NAME("gui.wars.moneyinput.opponent_name"),
        WGUI_MONEYINPUT_OPPONENT_CLAN("gui.wars.moneyinput.opponent_clan"),
        WGUI_MONEYINPUT_OPPONENT_BALANCE("gui.wars.moneyinput.opponent_balance"),
        WGUI_MONEYINPUT_INCREASE_NAME("gui.wars.moneyinput.increase_name"),
        WGUI_MONEYINPUT_INCREASE_LORE("gui.wars.moneyinput.increase_lore"),
        WGUI_MONEYINPUT_DECREASE_NAME("gui.wars.moneyinput.decrease_name"),
        WGUI_MONEYINPUT_DECREASE_LORE("gui.wars.moneyinput.decrease_lore"),
        WGUI_MONEYINPUT_STEP_NAME("gui.wars.moneyinput.step_name"),
        WGUI_MONEYINPUT_STEP_LEFT("gui.wars.moneyinput.step_left"),
        WGUI_MONEYINPUT_STEP_RIGHT("gui.wars.moneyinput.step_right"),
        WGUI_MONEYINPUT_SEND_NAME("gui.wars.moneyinput.send_name"),
        WGUI_MONEYINPUT_SEND_LORE("gui.wars.moneyinput.send_lore"),
        WGUI_MONEYINPUT_CANCEL_NAME("gui.wars.moneyinput.cancel_name"),
        WGUI_MONEYINPUT_CANCEL_LORE("gui.wars.moneyinput.cancel_lore"),

        WGUI_XPINPUT_TITLE("gui.wars.xpinput.title"),
        WGUI_XPINPUT_AMOUNT_NAME("gui.wars.xpinput.amount_name"),
        WGUI_XPINPUT_AMOUNT_OPPONENT("gui.wars.xpinput.amount_opponent"),
        WGUI_XPINPUT_AMOUNT_PRESTIGE("gui.wars.xpinput.amount_prestige"),
        WGUI_XPINPUT_AMOUNT_STEP("gui.wars.xpinput.amount_step"),
        WGUI_XPINPUT_OPPONENT_NAME("gui.wars.xpinput.opponent_name"),
        WGUI_XPINPUT_OPPONENT_CLAN("gui.wars.xpinput.opponent_clan"),
        WGUI_XPINPUT_OPPONENT_PRESTIGE("gui.wars.xpinput.opponent_prestige"),
        WGUI_XPINPUT_INCREASE_NAME("gui.wars.xpinput.increase_name"),
        WGUI_XPINPUT_INCREASE_LORE("gui.wars.xpinput.increase_lore"),
        WGUI_XPINPUT_DECREASE_NAME("gui.wars.xpinput.decrease_name"),
        WGUI_XPINPUT_DECREASE_LORE("gui.wars.xpinput.decrease_lore"),
        WGUI_XPINPUT_STEP_NAME("gui.wars.xpinput.step_name"),
        WGUI_XPINPUT_STEP_LEFT("gui.wars.xpinput.step_left"),
        WGUI_XPINPUT_STEP_RIGHT("gui.wars.xpinput.step_right"),
        WGUI_XPINPUT_SEND_NAME("gui.wars.xpinput.send_name"),
        WGUI_XPINPUT_SEND_LORE("gui.wars.xpinput.send_lore"),
        WGUI_XPINPUT_CANCEL_NAME("gui.wars.xpinput.cancel_name"),
        WGUI_XPINPUT_CANCEL_LORE("gui.wars.xpinput.cancel_lore"),

        WGUI_WARLORE_SIDE_OFFENSIVE("gui.wars.warlore.side_offensive"),
        WGUI_WARLORE_SIDE_DEFENSIVE("gui.wars.warlore.side_defensive"),
        WGUI_WARLORE_SIDE_OBSERVER("gui.wars.warlore.side_observer"),
        WGUI_WARLORE_SCORE("gui.wars.warlore.score"),
        WGUI_TERM_SIDE_MOFF("gui.wars.terms.side_moff"),
        WGUI_TERM_SIDE_MDEF("gui.wars.terms.side_mdef"),
        WGUI_TERM_SIDE_OFF("gui.wars.terms.side_off"),
        WGUI_TERM_SIDE_DEF("gui.wars.terms.side_def"),
        WGUI_TERM_WHITEPEACE("gui.wars.terms.whitepeace"),
        WGUI_TERM_MONEY("gui.wars.terms.money"),
        WGUI_TERM_XP("gui.wars.terms.xp"),
        WGUI_TERM_DISBAND("gui.wars.terms.disband"),
        WGUI_TERM_UNIFY("gui.wars.terms.unify"),

        REL_NO_AVAILABLE_CLANS("errors.relations.no_available_clans"),
        REL_INVALID_TARGET("errors.relations.invalid_target"),
        REL_IS_RELATION("errors.relations.is_relation"),
        REL_ALREADY_RELATION("errors.relations.already_relation"),
        REL_MAX_ALLIANCES("errors.relations.max_alliances"),
        REL_REQUEST_ALREADY_SENT("errors.relations.request_already_sent"),
        REL_REQUEST_INBOX_FULL("errors.relations.request_inbox_full"),

        REL_REQUEST_SENT("info.relations.request_sent"),
        REL_NEW_REQUEST_ONLINE("info.relations.new_request_online"),
        REL_NEW_REQUEST_BUTTON("info.relations.new_request_button"),
        REL_NEW_REQUEST_OFFLINE("info.relations.new_request_offline"),
        REL_ESTABLISHED_ALLY_BROADCAST("info.relations.established_ally_broadcast"),
        REL_ESTABLISHED_ENEMY_BROADCAST("info.relations.established_enemy_broadcast"),
        REL_REMOVED_ALLY_BROADCAST("info.relations.removed_ally_broadcast"),
        REL_REMOVED_ENEMY_BROADCAST("info.relations.removed_enemy_broadcast"),
        REL_ALLY_OFFLINE_NOTIFY("info.relations.ally_offline_notify"),
        REL_ENEMY_OFFLINE_NOTIFY("info.relations.enemy_offline_notify"),
        REL_REQUEST_REJECTED("info.relations.request_rejected"),
        REL_CLAN_HEAD_ERROR("info.relations.clan_head_error"),

        RGUI_MAIN_TITLE("gui.relations.main.title"),
        RGUI_MAIN_ALLIES_NAME("gui.relations.main.allies.name"),
        RGUI_MAIN_ALLIES_LORE("gui.relations.main.allies.lore"),
        RGUI_MAIN_ENEMIES_NAME("gui.relations.main.enemies.name"),
        RGUI_MAIN_ENEMIES_LORE("gui.relations.main.enemies.lore"),
        RGUI_MAIN_REQUESTS_NAME("gui.relations.main.requests.name"),
        RGUI_MAIN_REQUESTS_LORE("gui.relations.main.requests.lore"),
        RGUI_MAIN_SEND_ALLY_NAME("gui.relations.main.send_ally.name"),
        RGUI_MAIN_SEND_ALLY_LORE("gui.relations.main.send_ally.lore"),
        RGUI_MAIN_ADD_ENEMY_NAME("gui.relations.main.add_enemy.name"),
        RGUI_MAIN_ADD_ENEMY_LORE("gui.relations.main.add_enemy.lore"),

        RGUI_SELECTOR_TITLE_ALLY("gui.relations.selector.title_ally"),
        RGUI_SELECTOR_TITLE_ENEMY("gui.relations.selector.title_enemy"),
        RGUI_SELECTOR_PAGE_LORE("gui.relations.selector.page_lore"),

        RGUI_REQUESTS_TITLE("gui.relations.requests.title"),
        RGUI_REQUESTS_EMPTY_NAME("gui.relations.requests.empty_name"),
        RGUI_REQUESTS_EMPTY_LORE("gui.relations.requests.empty_lore"),
        RGUI_REQUESTS_ITEM_LORE("gui.relations.requests.item_lore"),

        RGUI_LIST_TITLE_ALLIES("gui.relations.list.title_allies"),
        RGUI_LIST_TITLE_ENEMIES("gui.relations.list.title_enemies"),
        RGUI_LIST_EMPTY_NAME("gui.relations.list.empty_name"),
        RGUI_LIST_EMPTY_LORE("gui.relations.list.empty_lore"),
        RGUI_LIST_ITEM_LORE("gui.relations.list.item_lore"),
        RGUI_LIST_REMOVE_HINT_ALLY("gui.relations.list.remove_hint_ally"),
        RGUI_LIST_REMOVE_HINT_ENEMY("gui.relations.list.remove_hint_enemy"),
        RGUI_CLAN_HEAD_LORE("gui.relations.clan_head.lore"),

        ADMIN_HELP("admin.help"),
        ADMIN_REPORTS_CONSOLE_CLAN_HEADER("admin.reports.console_clan_header"),
        ADMIN_REPORTS_CONSOLE_LINE("admin.reports.console_line"),
        ADMIN_REPORTS_GUI_TITLE("admin.reports.gui_title"),
        ADMIN_REPORTS_GUI_ITEM_NAME("admin.reports.gui_item_name"),
        ADMIN_REPORTS_GUI_ITEM_LORE_LINE("admin.reports.gui_item_lore_line"),

        USERS_HELP("users.help"),

        PAPI_NO_CLAN("placeholderapi.no-clan"),
        PAPI_NO_CLAN_FILE("placeholderapi.no-clan-file"),
        PAPI_EMPTY_FIELD("placeholderapi.empty-field"),
        PAPI_NO_PLAYER("placeholderapi.no-player");

        private final String path;

        Paths(String path) {
            this.path = path;
        }
    }

    public MSG(CX plugin) {
        MSG.plugin = plugin;
    }

    public static String send(Paths paths, String... args) {
        FileHandler fh = plugin.getFH();
        FileConfiguration msg = fh.getMsg();
        String raw = msg.getString(paths.path, "&4&lMESSAGE NOT FOUND");

        for (int i = 0; i < args.length; i++)
            raw = raw.replace("$$ph" + (i + 1) + "$$", args[i]);

        return Text.color(prefix + raw);
    }

    public static String sendWithoutPrefix(Paths paths, String... args) {
        FileHandler fh = plugin.getFH();
        FileConfiguration msg = fh.getMsg();
        String raw = msg.getString(paths.path, "&4&lMESSAGE NOT FOUND");

        for (int i = 0; i < args.length; i++)
            raw = raw.replace("$$ph" + (i + 1) + "$$", args[i]);

        return Text.color(raw);
    }

    public static List<String> sendList(Paths paths, String... args) {
        FileHandler fh = plugin.getFH();
        FileConfiguration msg = fh.getMsg();
        List<String> raw = msg.getStringList(paths.path);
        List<String> result = new ArrayList<>();

        for (String line : raw) {
            for (int i = 0; i < args.length; i++)
                line = line.replace("$$ph" + (i + 1) + "$$", args[i]);
            result.add(line);
        }

        return Text.colorList(result);
    }
}