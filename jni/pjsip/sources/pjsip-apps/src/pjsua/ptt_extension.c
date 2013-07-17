
/**************************************************************
* Copyright (C), 2012-201x, ZZY Tech. Co., Ltd.
* Project:   ZZY PTT 1.0
* File name: ptt_extension.c
* Author:    ZhangZhiChao
* Version:   v1.0
* Date:      2012.03
* Description: ptt multicast communication relation interface
**************************************************************/

#include <pjsua-lib/pjsua.h>
#include <pj/compat/socket.h>
#include "pjsip-jni.h"
#include "ptt_extension.h"


enum
{
    PTT_SUCCESS,

    SYNTAX_ERROR = 1

};

typedef enum
{
    PTT_OFFLINE,

    PTT_LISTENING,

    PTT_SPEAKING,

    PTT_ONLINE,

    MAX_STATE

}member_state;

typedef struct ptt_member_info
{
    PJ_DECL_LIST_MEMBER(struct ptt_member_info);

    /** group member number. */
    pj_str_t number;

    /** group member name. */
    pj_str_t name;

    member_state state;

}ptt_member_info;

#define THIS_FILE	"ptt_extension.c"
#define PTT_INVALIDATE     -1

#define GROUP_NUM_LENGTH 32
#define GROUP_NAME_LENGTH 32
#define MEMBER_NAME_LENGTH 32
#define MEMBER_NUM_LENGTH 32

#define TIMER_SCHEDULE 5 //5 secondes
#define PTT_TIMER_INVITE_MAX 5

static PTT_info ptt_info;
static ptt_status ptt_state;
static char packet_info[512];
static char g_speaker[512];
static char current_call_id[512];
static pjmedia_stream *stream = NULL;
static pjmedia_snd_port *snd_port = NULL;
static multicast_INVITE call_info;
ptt_group_info * g_current_group;
static unsigned ptt_timer_count;//9* TIMER_SCHEDULE seconds
static unsigned ptt_timer_invite_count;//PTT_TIMER_INVITE_MAX* TIMER_SCHEDULE seconds

extern unsigned sipcall_timer_count;
extern char switch_addr[PJ_INET_ADDRSTRLEN];

static void multicast_invite_indicate(pj_str_t * group_num, pj_str_t *speaker);
static void multicast_bye_indicate(void);
#ifdef PJSIP_EXTEND_ZED
static void update_ptt_state(multicast_INFO *info);
#endif
#ifdef PJSIP_EXTEND_ZZY
static void update_ptt_state(multicast_INVITE *inv);
#endif
static void on_scanner_error(pj_scanner *scanner)
{
    PJ_UNUSED_ARG(scanner);

    PJ_THROW(SYNTAX_ERROR);
}

static void ptt_group_info_parse(pj_scanner *scanner, ptt_group_info *group_info)
{
    pj_str_t out;

    pj_scan_get_until_ch(scanner, ',', &out);
    pj_strdup_with_null(group_info->own_pool, &group_info->name, &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ',', &out);
    pj_strdup_with_null(group_info->own_pool, &group_info->number, &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ',', &out);
    group_info->level = pj_strtoul(&out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ',', &out);
    group_info->report_heartbeat = pj_strtoul(&out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ',', &out);
    group_info->update_heartbeat = pj_strtoul(&out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ':', &out);
    pj_strdup_with_null(group_info->own_pool, &group_info->mcast_address, &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, ";\r\n", &out);
    group_info->port = pj_strtoul(&out);
    group_info->transport = NULL;
    group_info->prev = group_info->next = group_info;
}

static void ptt_member_info_parse(pj_scanner *scanner, ptt_member_info *member_info)
{
    pj_str_t out;

    pj_scan_get_until_ch(scanner, ',', &member_info->number);
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ',', &member_info->name);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, ";)", &out);
    member_info->state = pj_strtoul(&out);
}

static pj_status_t manage_media_group(ptt_group_info *group_info, pj_uint16_t optname)
{
    pj_status_t status;
    struct ip_mreq mc_req;
    pjmedia_transport_info info = {0};
    char hostbuf[PJ_INET6_ADDRSTRLEN];

    pjmedia_transport_get_info(group_info->transport, &info);
    pj_bzero(hostbuf, sizeof(hostbuf));
    pj_ansi_strncpy(hostbuf, group_info->mcast_address.ptr, group_info->mcast_address.slen);

    mc_req.imr_multiaddr.s_addr = inet_addr(hostbuf);
    if(switch_addr[0])
    {
        PJ_LOG(4,(THIS_FILE, "======media interface addr %s======", switch_addr));
        mc_req.imr_interface.s_addr = inet_addr(switch_addr);
    }
    else
    {
        mc_req.imr_interface.s_addr = htonl(INADDR_ANY);
    }

    PJ_LOG(4,(THIS_FILE, "======media opt: %d, media_address %s======", optname, hostbuf));
    status = pj_sock_setsockopt(info.sock_info.rtp_sock, IPPROTO_IP, optname, (char*) &mc_req, sizeof(mc_req));
    if (status != PJ_SUCCESS)
    {
        pjsua_perror(THIS_FILE, "Error SET SOCKOPT", status);
    }
    return status;
}

static int ptt_media_transport_create(ptt_group_info *group_info)
{
    ptt_group_info * node;
    pj_status_t status;

    if (!group_info)
    {
        return PJ_SUCCESS;
    }

    node = group_info;
    do
    {
        if (!node->transport && node->number.ptr)
        {
            status = pjmedia_transport_udp_create(pjsua_get_pjmedia_endpt(), node->number.ptr, node->port, 0, &node->transport);
            PJ_LOG(3, (THIS_FILE, "ptt_media_transport_create %d", status));
            status = manage_media_group(node, IP_ADD_MEMBERSHIP);
        }
        node = node->next;
    } while (node != group_info);

    return PJ_SUCCESS;
}

static int ptt_media_transport_destroy(ptt_group_info *group_info, pj_bool_t bforce)
{
    ptt_group_info * node;
    ptt_group_info * node_erase;

    if (!group_info)
    {
        return PJ_SUCCESS;
    }

    if (bforce)
    {
        if (snd_port)
        {
            pjmedia_snd_port_destroy(snd_port);
            snd_port = NULL;
        }

        if (ptt_info.snd_pool)
            pj_pool_release(ptt_info.snd_pool);
        ptt_info.snd_pool = NULL;

        if (stream)
        {
            pjmedia_stream_destroy(stream);
            stream = NULL;
        }
    }

    node = group_info;
    do
    {
        if (node->transport)
        {
            pjmedia_transport_close(node->transport);
            node->transport= NULL;
        }
        node_erase = node;
        node = node->next;
        if (node_erase->own_pool)
        {
            pj_list_erase(node_erase);
            pj_pool_release(node_erase->own_pool);
        }

    } while (node != node_erase);

    return PJ_SUCCESS;
}

static pj_status_t manage_signal_group(pjsip_transport * transport, pj_uint16_t optname)
{
    pj_status_t status;
    struct ip_mreq mc_req;
    char hostbuf[PJ_INET6_ADDRSTRLEN];

    pj_bzero(hostbuf, sizeof(hostbuf));
    pj_ansi_strncpy(hostbuf, transport->local_name.host.ptr, transport->local_name.host.slen);

    mc_req.imr_multiaddr.s_addr = inet_addr(hostbuf);
    if(switch_addr[0])
    {
        PJ_LOG(4,(THIS_FILE, "======signal interface addr %s======", switch_addr));
        mc_req.imr_interface.s_addr = inet_addr(switch_addr);
    }
    else
    {
        mc_req.imr_interface.s_addr = htonl(INADDR_ANY);
    }

    PJ_LOG(4,(THIS_FILE, "======signal opt: %d signal_address : %s======", optname, hostbuf));
    status = pj_sock_setsockopt(pjsip_udp_transport_get_socket(transport), IPPROTO_IP, optname, (char*) &mc_req, sizeof(mc_req));
    if (status != PJ_SUCCESS)
    {
        pjsua_perror(THIS_FILE, "Error SET SOCKOPT", status);
    }
    return status;
}

static void ptt_enter_state(ptt_status new_state)
{
    PJ_LOG(3, (THIS_FILE, "ptt_enter_state from %d to %d", ptt_state, new_state));
    ptt_state = new_state;
}

static void do_cleanup_on_timeout(pj_bool_t bin_ptt)
{
     PJ_LOG(3,(THIS_FILE, ">>>>>>>>> do_cleanup_on_timeout, ptt_state %d call_count %d in_ptt %d",ptt_state,pjsua_call_get_count(), bin_ptt));
    if (ptt_state > PTT_STATUS_UNINIT && pjsua_call_get_count() == 0)
    {
        ptt_close_media();
        pj_bzero(&call_info, sizeof(multicast_INVITE));
        pj_bzero(current_call_id, 512);
        pj_bzero(g_speaker, 512);
        ptt_enter_state(PTT_STATUS_UNINIT);
        g_current_group = NULL;
        multicast_invite_indicate(NULL, NULL);//in case the ui has opened and no way to close it
    }
    else if(!bin_ptt)
    {
        if(pjsua_call_get_count() >0)
            pjsua_call_hangup_all();
        multicast_invite_indicate(NULL, NULL);//in case the ui has opened and no way to close it
    }
    
}

static void ptt_signal_timer_cb(pj_timer_heap_t *th, pj_timer_entry *te)
{
    pj_time_val delay;
    int i,max;
    pjsua_call_info call_info;

    PJ_UNUSED_ARG(th);

    max = pjsua_call_get_max_count();
    for (i = 0; i < max; ++i)
    {
        if (pjsua_call_is_active(i))
        {
            pjsua_call_get_info(i, &call_info);
            if (call_info.state > PJSIP_INV_STATE_NULL && call_info.state < PJSIP_INV_STATE_CONFIRMED && call_info.total_duration.sec > 60)
            {
                pjsua_call_hangup(i, 0, NULL, NULL);
            }
        }
    }

    ptt_timer_count++;
    if (ptt_timer_count > 1 && ptt_timer_count%3 == 1)
    {
        manage_ptt_group(IP_DROP_MEMBERSHIP);
        manage_ptt_group(IP_ADD_MEMBERSHIP);
    }

    if (ptt_timer_count == 9)
    {
        ptt_timer_count = 0;
        //do_cleanup_on_timeout(PJ_FALSE);
    }

    //added by lxd
    ptt_timer_invite_count++;
    if(ptt_timer_invite_count == PTT_TIMER_INVITE_MAX)
    {
        ptt_timer_invite_count = 0;
        //PJ_LOG(3,(THIS_FILE, ">>>>>>>>> invite time out, ptt_state %d \n", ptt_state));
        //do_cleanup_on_timeout(PJ_TRUE);
    }
    
    te->id = PJ_FALSE;
    delay.sec = TIMER_SCHEDULE;//multicast heartbeat
    delay.msec = 0;
    if (pjsip_endpt_schedule_timer(pjsua_get_pjsip_endpt(), te, &delay) == PJ_SUCCESS)
    {
        te->id = PJ_TRUE;
    }
    else
    {
        pjsua_perror(THIS_FILE, "Error starting ptt_signal timer", te->id);
    }
}

static int ptt_transport_create(pj_str_t *mcast_address, unsigned port, pjsua_transport_id *transport_id)
{
    pj_time_val delay;
    pj_status_t status;
    pjsua_transport_config config;
    pjsip_transport_type_e type = PJSIP_TRANSPORT_MULTICAST;

    pjsua_transport_config_default(&config);
    config.port = port;
    config.public_addr = *mcast_address;
    status = pjsua_transport_create(type, &config, transport_id);
    manage_signal_group(pjsua_get_transport(*transport_id), IP_ADD_MEMBERSHIP);

    ptt_info.signal_timer.id = PJ_FALSE;
    ptt_info.signal_timer.cb = &ptt_signal_timer_cb;
    ptt_info.signal_timer.user_data = transport_id;
    delay.sec = TIMER_SCHEDULE;//multicast heartbeat
    delay.msec = 0;

    status = pjsip_endpt_schedule_timer(pjsua_get_pjsip_endpt(), &ptt_info.signal_timer, &delay);
    if (status == PJ_SUCCESS)
    {
        ptt_info.signal_timer.id = PJ_TRUE;
    }

    return status;
}

static int ptt_transport_destroy(pjsua_transport_id transport_id)
{
    pj_status_t status = PJ_SUCCESS;

    if (transport_id != PTT_INVALIDATE)
    {
        status = pjsua_transport_close(transport_id, PJ_TRUE);
    }

    if (ptt_info.signal_timer.id)
    {
        pjsip_endpt_cancel_timer(pjsua_get_pjsip_endpt(), &ptt_info.signal_timer);
        ptt_info.signal_timer.id = PJ_FALSE;
    }

    return status;
}

static void group_info_init(reg_content *group_info)
{
    group_info->general_group = NULL;
    if (group_info->emergency_group.prev == group_info->emergency_group.next && group_info->emergency_group.next == NULL)
        pj_list_init(&group_info->emergency_group);
    group_info->signal_address.transport_id = PTT_INVALIDATE;
}

static void group_info_destroy(reg_content *group_info)
{
    PJ_LOG(3, (THIS_FILE, ">>>>group_info_destroy"));
    ptt_media_transport_destroy(group_info->general_group, PJ_TRUE);
    group_info->general_group = NULL;
    ptt_transport_destroy(group_info->signal_address.transport_id);
    group_info->emergency_group.prev = group_info->emergency_group.next = NULL;
    group_info->signal_address.transport_id = PTT_INVALIDATE;
}

static pj_status_t multicast_call_incoming(ptt_group_info *group_info, pj_uint8_t codec)
{
    pj_status_t status;
    pjmedia_endpt *med_endpt;
    pjmedia_stream_info info;
    pjmedia_transport *transport = NULL;
    const pjmedia_codec_info *codec_info;
    pjmedia_port *stream_port;

    med_endpt = pjsua_get_pjmedia_endpt();
    pj_bzero(&info, sizeof(info));
    info.type = PJMEDIA_TYPE_AUDIO;
    info.dir = PJMEDIA_DIR_DECODING;
    if (codec == 0xff)
        codec = 3;
    PJ_LOG(3, (THIS_FILE, ">>>>>>multicast_call_incoming codec  %d", codec));
    pjmedia_codec_mgr_get_codec_info(pjmedia_endpt_get_codec_mgr(med_endpt), codec, &codec_info);
    pj_memcpy(&info.fmt, codec_info, sizeof(pjmedia_codec_info));
    info.rx_pt = codec_info->pt;
    info.ssrc = pj_rand();

    transport = group_info->transport;
    if(transport == NULL)
    {
        status = ptt_media_transport_create(group_info);
        PJ_LOG(3, (THIS_FILE, "Error transport %d status %d", transport, status));
        if(status != PJ_SUCCESS)//lxd add 2013-01-25 11:40
            return -1;
    }
    pj_sockaddr_in_init(&info.rem_addr, &group_info->mcast_address, group_info->port);//NULL
    status = pjmedia_stream_create(med_endpt, NULL, &info, transport, NULL, &stream);
    if (status != PJ_SUCCESS) {
        pjsua_perror(THIS_FILE, "Error creating stream", status);
        pjmedia_transport_close(transport);
        return status;
    }
    /* Get the port interface of the stream */
    status = pjmedia_stream_get_port(stream, &stream_port);
    PJ_ASSERT_RETURN(status == PJ_SUCCESS, PJ_ENOTFOUND);

    ptt_info.snd_pool = pjsua_pool_create("ptt_snd", 3000, 3000);
    PJ_ASSERT_RETURN(ptt_info.snd_pool, PJ_ENOMEM);
    status = pjmedia_snd_port_create_player(ptt_info.snd_pool, -1, codec_info->clock_rate, codec_info->channel_cnt, 160, 16,
        0, &snd_port);
    if (status != PJ_SUCCESS) {
        pjsua_perror(THIS_FILE, "Unable to create sound port", status);
        return status;
    }

    /* Connect sound port to stream */
    status = pjmedia_snd_port_connect(snd_port, stream_port);
    pjmedia_stream_start(stream);
    return status;
}

static void multicast_call_ending(ptt_group_info *group_info)
{
    if (snd_port)
    {
        pjmedia_snd_port_destroy(snd_port);
        snd_port = NULL;
    }

    if (ptt_info.snd_pool)
        pj_pool_release(ptt_info.snd_pool);
    ptt_info.snd_pool = NULL;

    if (stream)
    {
        pjmedia_stream_destroy(stream);
        stream = NULL;
    }
}

static pj_bool_t multicast_info_compare(ptt_group_info *group_info, pj_str_t *group_num)
{
    if (pj_strcmp(&group_info->number, group_num))
        return PJ_FALSE;
    else
        return PJ_TRUE;
}

static pj_bool_t check_speaker(pj_str_t * speaker)
{
    int status = -1;

    pjsua_acc_config acc_cfg;

    pjsua_acc_id acc_id = pjsua_acc_get_default();

    status = pjsua_acc_get_config(acc_id, &acc_cfg);
    if(status == PJ_SUCCESS)
    {
        if(!pj_strcmp(&acc_cfg.cred_info[0].username, speaker))
            return PJ_TRUE;
        else
            return PJ_FALSE;
    }
    else
        return PJ_FALSE;


}

static void receive_member_state_change(multicast_MemberState *member_info);

static pj_bool_t multicast_mod_on_rx_request(pjsip_rx_data *rdata)
{
    multicast_signal multicast_info = rdata->multicast_info;
    ptt_timer_count = 0;
    sipcall_timer_count = 0;

    //lxd commented
    PJ_LOG(3, (THIS_FILE, ">>>>>>sxsexe multicast_mod_on_rx_request should not show "));

    switch(multicast_info.type)
    {
    case TYPE_HEARTBEAT:
        {
            //check the general group info in the heartbeat group info
            multicast_HEART heartbeat;
            heart_beat_group_info *node;
            ptt_timer_count = 0;
            sipcall_timer_count = 0;
            
            heartbeat = multicast_info.multi.heart_beat;
            if(g_current_group)
            {
                pj_bool_t bfind = PJ_FALSE;
                node = heartbeat.header;
                
                if(node)
                {
                    do
                    {
                        if(!pj_strncmp(&node->group_num, &g_current_group->number, g_current_group->number.slen))
                        {
                            bfind = PJ_TRUE;
                            break;
                        }
                        node = node->next;
                    }while(node != heartbeat.header);
                }
                else
                {
                    //added by lxd 
                    //fix bug when in ptt_lifecycle, 
                    if(ptt_state > PTT_STATUS_UNINIT)
                        bfind = PJ_TRUE;
                    else
                        bfind = PJ_FALSE;
                }
                
                if(bfind)
                {
                    //do nothing
                }
                else
                {
                   //force close ptt
                    PJ_LOG(3, (THIS_FILE, ">>>>> force close ptt cause no group found in heartbeat"));
                    multicast_bye_indicate();
                    ptt_close_media();
                    pj_bzero(&call_info, sizeof(multicast_INVITE));
                    pj_bzero(current_call_id, 512);
                    pj_bzero(g_speaker, 512);
                    ptt_enter_state(PTT_STATUS_UNINIT);
                    g_current_group = NULL;
                }
            }
            if(heartbeat.header)
            {
                pj_pool_release(heartbeat.header->own_pool);
            }
        }
        break;
    case TYPE_INVITE:
    {
        multicast_INVITE inv;
        ptt_group_info * node;
        pj_bool_t bfind = PJ_FALSE;
        pj_bool_t b_speaker_changed = PJ_FALSE;

        inv = multicast_info.multi.inv;

        if(g_speaker)
        {
            if(!pj_ansi_strncmp(g_speaker, inv.speaker.ptr, inv.speaker.slen))
            {
                b_speaker_changed = PJ_FALSE;
            }
            else
            {
                pj_ansi_strncpy(g_speaker, inv.speaker.ptr, inv.speaker.slen);
                b_speaker_changed = PJ_TRUE;
            }
        }
        else
        {
            pj_ansi_strncpy(g_speaker, inv.speaker.ptr, inv.speaker.slen);
            b_speaker_changed = PJ_TRUE;
        }

        node = ptt_info.group_info.general_group;
        if (!node)
            break;
        do
        {
            if (!pj_strcmp(&node->number, &inv.group_num))
            {
                if (g_current_group)
                {
                    if (!pj_strcmp(&g_current_group->number, &inv.group_num))
                    {
                        bfind = PJ_TRUE;
                    }
                    else
                    {
                        //TODO:
                    }
                }
                else
                {
                    bfind = PJ_TRUE;
                }
                break;
            }
            node = node->next;

        } while (node != ptt_info.group_info.general_group);

        if (bfind)
        {
            ptt_timer_invite_count = 0;
        #ifdef PJSIP_EXTEND_ZED
            call_info.call_id = inv.call_id;
            call_info.codec = inv.codec;
        #endif
        #ifdef PJSIP_EXTEND_ZZY
            call_info.codec = inv.codec;
            pj_ansi_strncpy(current_call_id, inv.call_id.ptr, inv.call_id.slen);
            call_info.call_id = pj_str(current_call_id);
        #endif
            if (ptt_state >= PTT_STATUS_UNINIT)
            {
                PJ_LOG(3, (THIS_FILE, ">>>>>>multicast_mod_on_rx_request TYPE_INVITE ptt_state %d", ptt_state));
                if(inv.speaker.slen == 0)
                {
                    pj_bzero(g_speaker, 512);
                    update_ptt_state(&inv);
                    if(stream)
                    {
                        pjmedia_stream_pause(stream, PJMEDIA_DIR_PLAYBACK);
                    }
                    ptt_enter_state(PTT_STATUS_FREE);
                }
                else
                {
                    pj_str_t number;
                    if(check_speaker(&inv.speaker))
                    {
                        if(stream)
                        {
                            pjmedia_stream_resume(stream, PJMEDIA_DIR_PLAYBACK);
                        }
                        return PJ_TRUE;
                    }
                    
                    if(g_current_group)
                    {
                        g_current_group = node;
                        if(stream)
                        {
                            pjmedia_stream_resume(stream, PJMEDIA_DIR_PLAYBACK);
                        }
                        else
                        {
                            ptt_enter_state(PTT_STATUS_INIT);
                            if(pjsua_call_get_count() == 0)
                                ptt_open_media();
                        }
                        get_dest_number(&number);
                        if(pjsua_call_get_count() != 0 && pj_strcmp(&number, &inv.group_num))
                            ptt_enter_state(PTT_STATUS_FREE);
                        else
                            ptt_enter_state(PTT_STATUS_BUSY);
                    }
                    else
                    {
                        g_current_group = node;
                        ptt_enter_state(PTT_STATUS_INIT);
                        if(pjsua_call_get_count() == 0)
                            ptt_open_media();
                    }

                    get_dest_number(&number);
                    if (pj_strcmp(&number, &inv.group_num) || b_speaker_changed)
                        multicast_invite_indicate(&inv.group_num, &inv.speaker);
                    else
                        PJ_LOG(4,(THIS_FILE, "========= make group call ========="));
                }
            }
        }
    }
        break;
    case TYPE_BYE:
        if (ptt_state != PTT_STATUS_UNINIT)
        {
            multicast_BYE bye;
            bye = multicast_info.multi.bye;
#ifdef PJSIP_EXTEND_ZED
            if (bye.call_id == call_info.call_id)
#endif
#ifdef PJSIP_EXTEND_ZZY
            if (!pj_ansi_strncmp(bye.call_id.ptr, current_call_id, bye.call_id.slen))
#endif
            {
                multicast_bye_indicate();
                ptt_close_media();
                pj_bzero(&call_info, sizeof(multicast_INVITE));
                pj_bzero(current_call_id, 512);
                pj_bzero(g_speaker, 512);
                ptt_enter_state(PTT_STATUS_UNINIT);
                g_current_group = NULL;
            }
        }
        break;

    case TYPE_MEMBER_STATE:
        {
            multicast_MemberState member_status;
            ptt_member_info member_info;
            member_status = multicast_info.multi.member_state;
            if(pj_ansi_strncmp("all", member_status.num.ptr, member_status.num.slen))
            {
                receive_member_state_change(&member_status);
            }
            else
            {
                receive_member_state_change(NULL);
            }
        }
        break;
    default:
        break;
    }
    return PJ_TRUE;
}

/* The module instance. */
static pjsip_module mod_multicast_handler =
{
    NULL, NULL,				/* prev, next.		*/
    { "mod-multicast-handler", 21 },	/* Name.		*/
    -1,					/* Id			*/
    PJSIP_MOD_PRIORITY_APPLICATION,	/* Priority	        */
    NULL,				/* load()		*/
    NULL,				/* start()		*/
    NULL,				/* stop()		*/
    NULL,				/* unload()		*/
    &multicast_mod_on_rx_request,		/* on_rx_request()	*/
    NULL,				/* on_rx_response()	*/
    NULL,				/* on_tx_request.	*/
    NULL,				/* on_tx_response()	*/
    NULL,				/* on_tsx_state()	*/

};


extern jclass jni_callback_class;
extern jclass group_info_class;

static void receive_group_info(ptt_group_info *group_info)
{
    jmethodID rcvGrpInfo, constructorMethodId;
    jobject obj_jni_callback, obj_group_info;
    jobjectArray group_infos;
    jmethodID setName, setNumber, setLevel;
    jstring strNumber, strName;
    jchar level;
    ptt_group_info *p_list_group_info;
    char str[32];
    int i = 0;

    JNIEnv *jni_env = 0;

    if (!jni_callback_class || !group_info_class)
        return;

    ATTACH_JVM(jni_env);

    jclass Cls = (*jni_env)->FindClass(jni_env,"java/lang/Object");
    if (Cls == NULL)
    {
        PJ_LOG(2, (THIS_FILE, "Not able to find Object class"));
        DETACH_JVM(jni_env);
        return;
    }
    if (group_info)
    {
        group_infos =  (*jni_env)->NewObjectArray(jni_env, (jsize)(pj_list_size(group_info)+1), Cls , 0);

        p_list_group_info = group_info;
        do
        {
            constructorMethodId = (*jni_env)->GetMethodID(jni_env, group_info_class, "<init>", "()V");
            obj_group_info = (*jni_env)->NewObject(jni_env, group_info_class, constructorMethodId);
            setName = (*jni_env)->GetMethodID(jni_env, group_info_class, "setName", "(Ljava/lang/String;)V");
            setNumber = (*jni_env)->GetMethodID(jni_env, group_info_class, "setNumber", "(Ljava/lang/String;)V");
            setLevel = (*jni_env)->GetMethodID(jni_env, group_info_class, "setLevel", "(C)V");

            pj_bzero(str, sizeof(str));
            pj_ansi_strncpy(str, p_list_group_info->number.ptr, p_list_group_info->number.slen);
            strNumber = (*jni_env)->NewStringUTF(jni_env, str);
            pj_bzero(str, sizeof(str));
            pj_ansi_strncpy(str, p_list_group_info->name.ptr, p_list_group_info->name.slen);
            strName = (*jni_env)->NewStringUTF(jni_env, str);

            (*jni_env)->CallVoidMethod(jni_env, obj_group_info, setName, strName);
            (*jni_env)->CallVoidMethod(jni_env, obj_group_info, setNumber, strNumber);
            (*jni_env)->CallVoidMethod(jni_env, obj_group_info, setLevel, (jchar)(p_list_group_info->level));
            (*jni_env)->SetObjectArrayElement(jni_env, group_infos, i, obj_group_info);
            p_list_group_info = p_list_group_info->next;
            i++;
        }while(p_list_group_info != group_info);
    }
    else
    {
        group_infos = NULL;
    }

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    rcvGrpInfo = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "receiveGroupInfo", "([Lcom/zzy/ptt/model/GroupInfo;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, rcvGrpInfo, group_infos);

    DETACH_JVM(jni_env);
}

//added by lxd
extern jclass member_info_class;
static void receive_member_info(ptt_member_info *member_info, char *groupNum)
{
    jmethodID receiveMemberInfo, constructorMethodId;
    jobject obj_jni_callback, obj_member_info;
    jobjectArray member_infos;
    jmethodID setName, setNumber, setStatus;
    jstring strNumber, strName, strGroupNum;
    jint status;
    ptt_member_info *node;

    char name[MEMBER_NAME_LENGTH];
    char number[MEMBER_NUM_LENGTH];
    int i = 0;

    JNIEnv *jni_env = 0;

    if (!jni_callback_class || !member_info_class)
        return;

    ATTACH_JVM(jni_env);

    jclass Cls = (*jni_env)->FindClass(jni_env,"java/lang/Object");
    if (Cls == NULL)
    {
        PJ_LOG(2, (THIS_FILE, "Not able to find Object class"));
        DETACH_JVM(jni_env);
        return;
    }
    member_infos =  (*jni_env)->NewObjectArray(jni_env, (jsize)(pj_list_size(member_info)+1), Cls , 0);

    node = member_info;
    do {
        constructorMethodId = (*jni_env)->GetMethodID(jni_env, member_info_class, "<init>", "()V");
        obj_member_info = (*jni_env)->NewObject(jni_env, member_info_class, constructorMethodId);
        setName = (*jni_env)->GetMethodID(jni_env, member_info_class, "setName", "(Ljava/lang/String;)V");
        setNumber = (*jni_env)->GetMethodID(jni_env, member_info_class, "setNumber", "(Ljava/lang/String;)V");
        setStatus = (*jni_env)->GetMethodID(jni_env, member_info_class, "setStatus", "(I)V");

        pj_bzero(name, sizeof(name));
        pj_ansi_strncpy(name, node->name.ptr, node->name.slen);
        strName = (*jni_env)->NewStringUTF(jni_env, name);
        pj_bzero(number, sizeof(number));
        pj_ansi_strncpy(number, node->number.ptr, node->number.slen);
        strNumber = (*jni_env)->NewStringUTF(jni_env, number);

        (*jni_env)->CallVoidMethod(jni_env, obj_member_info, setName, strName);
        (*jni_env)->CallVoidMethod(jni_env, obj_member_info, setNumber, strNumber);
        (*jni_env)->CallVoidMethod(jni_env, obj_member_info, setStatus, (jint)(node->state));
        (*jni_env)->SetObjectArrayElement(jni_env, member_infos, i, obj_member_info);
        i++;
        node = node->next;
    }while(node != member_info);

    strGroupNum = (*jni_env)->NewStringUTF(jni_env, groupNum);

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    receiveMemberInfo = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "receiveMemberInfo", "(Ljava/lang/String;[Lcom/zzy/ptt/model/MemberInfo;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, receiveMemberInfo, strGroupNum, member_infos);

    DETACH_JVM(jni_env);
}

static void multicast_invite_indicate(pj_str_t *group_num, pj_str_t *speaker)
{
    jobject obj_jni_callback;
    jmethodID mOpenPttUI, constructorMethodId;
    char grp_num[GROUP_NUM_LENGTH] = {0};
    char speaker_num[GROUP_NUM_LENGTH] = {0};
    jstring str_group_num, str_speaker;
    JNIEnv *jni_env = 0;

    if (!jni_callback_class)
        return;

    ATTACH_JVM(jni_env);
    //PJ_LOG(3, (THIS_FILE, "sxsexe multicast_invite_indicate invite group_num : %s, speaker : %s\n",grp_num, speaker_num));

    if (group_num)
    {
        pj_ansi_strncpy(grp_num, group_num->ptr, group_num->slen);
        //PJ_LOG(3, (THIS_FILE, "invite group_num : %s, speaker : %s\n",grp_num, speaker_num));
        str_group_num = (*jni_env)->NewStringUTF(jni_env, grp_num);
        if(speaker)
        {
            pj_ansi_strncpy(speaker_num, speaker->ptr, speaker->slen);
            str_speaker = (*jni_env)->NewStringUTF(jni_env, speaker_num);
        }
        else
            str_speaker = NULL;
        
    }
    else
    {
        str_group_num = NULL;
        str_speaker = NULL;
    }
    PJ_LOG(3, (THIS_FILE, "sxsexe multicast_invite_indicate invite group_num : %s, speaker : %s\n",grp_num, speaker_num));
    
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    mOpenPttUI = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "mInviteIndicate", "(Ljava/lang/String;Ljava/lang/String;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mOpenPttUI, str_group_num, str_speaker);

    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    (*jni_env)->DeleteLocalRef(jni_env, str_group_num);
    (*jni_env)->DeleteLocalRef(jni_env, str_speaker);
    DETACH_JVM(jni_env);
}

static void multicast_invite_indicate2(char *group_num, char *speaker)
{
    jobject obj_jni_callback;
    jmethodID mOpenPttUI, constructorMethodId;
    jstring str_group_num, str_speaker;
    JNIEnv *jni_env = 0;

    if (!jni_callback_class)
        return;

    ATTACH_JVM(jni_env);
    PJ_LOG(3, (THIS_FILE, "sxsexe multicast_invite_indicate2 invite group_num : %s, speaker : %s\n",group_num, speaker));

    if (group_num)
    {
        //PJ_LOG(3, (THIS_FILE, "invite group_num : %s, speaker : %s\n",grp_num, speaker_num));
        str_group_num = (*jni_env)->NewStringUTF(jni_env, group_num);
        if(speaker)
        {
            str_speaker = (*jni_env)->NewStringUTF(jni_env, speaker);
        }
        else
            str_speaker = NULL;

    }
    else
    {
        str_group_num = NULL;
        str_speaker = NULL;
    }
    PJ_LOG(3, (THIS_FILE, "sxsexe multicast_invite_indicate2 invite group_num : %s, speaker : %s\n",group_num, speaker));

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    mOpenPttUI = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "mInviteIndicate", "(Ljava/lang/String;Ljava/lang/String;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mOpenPttUI, str_group_num, str_speaker);

    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    (*jni_env)->DeleteLocalRef(jni_env, str_group_num);
    (*jni_env)->DeleteLocalRef(jni_env, str_speaker);
    DETACH_JVM(jni_env);
}


static void multicast_bye_indicate(void)
{
    jobject obj_jni_callback;
    jmethodID mClosePttUI, constructorMethodId;

    JNIEnv *jni_env = 0;

    if (!jni_callback_class)
        return;
    ATTACH_JVM(jni_env);

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    mClosePttUI = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "mByeIndicate", "()V");

    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mClosePttUI);
    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    DETACH_JVM(jni_env);
}

#ifdef PJSIP_EXTEND_ZED
extern jclass ptt_group_status_class;
static void update_ptt_state(multicast_INFO *info)
{
    jobject obj_jni_callback, obj_ptt_group_status;
    jmethodID mUpdatePttStatus, constructorMethodId;
    char str[GROUP_NUM_LENGTH];
    jstring str_group_num, speaker_num, str_start_time, str_speaker_start_time;
    jmethodID mSetGroupNum, mSetCallId, mSetSpeaker, mSetStartTime, mSetSpkStartTime, mSetStatus;
    JNIEnv *jni_env = 0;

    if (!jni_callback_class || !ptt_group_status_class)
        return;
    ATTACH_JVM(jni_env);

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "<init>", "()V");
    obj_ptt_group_status = (*jni_env)->NewObject(jni_env, ptt_group_status_class, constructorMethodId);

    pj_bzero(str, GROUP_NUM_LENGTH);
    pj_ansi_strncpy(str, info->group_num.ptr, info->group_num.slen);
    str_group_num = (*jni_env)->NewStringUTF(jni_env, str);
    pj_bzero(str, GROUP_NUM_LENGTH);
    pj_ansi_strncpy(str, info->start_time.ptr, info->start_time.slen);
    str_start_time = (*jni_env)->NewStringUTF(jni_env, str);
    if (info->status == 2)
    {
        pj_bzero(str, GROUP_NUM_LENGTH);
        pj_ansi_strncpy(str, info->speaker.ptr, info->speaker.slen);
        speaker_num = (*jni_env)->NewStringUTF(jni_env, str);
        pj_bzero(str, GROUP_NUM_LENGTH);
        pj_ansi_strncpy(str, info->spk_start_time.ptr, info->spk_start_time.slen);
        str_speaker_start_time = (*jni_env)->NewStringUTF(jni_env, str);
    }

    mSetGroupNum = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setGroupNum", "(Ljava/lang/String;)V");
    mSetCallId = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setSeqCallId", "(I)V");
    mSetStartTime = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setStartTime", "(Ljava/lang/String;)V");
    mSetStatus = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setStatus", "(I)V");

    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetGroupNum, str_group_num);
    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetStartTime, str_start_time);
    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetStatus, info->status);
    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetCallId, info->call_id);
    if (info->status == 2)
    {
        mSetSpeaker = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setSpeakerNum", "(Ljava/lang/String;)V");
        mSetSpkStartTime = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setSpeakerStartTime", "(Ljava/lang/String;)V");
        (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetSpeaker, speaker_num);
        (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetSpkStartTime, str_speaker_start_time);
    }

    mUpdatePttStatus = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "updatePttState", "(Lcom/zzy/ptt/model/PttGroupStatus;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mUpdatePttStatus, obj_ptt_group_status);

    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    (*jni_env)->DeleteLocalRef(jni_env, obj_ptt_group_status);
    (*jni_env)->DeleteLocalRef(jni_env, str_group_num);
    (*jni_env)->DeleteLocalRef(jni_env, speaker_num);
    (*jni_env)->DeleteLocalRef(jni_env, str_start_time);
    (*jni_env)->DeleteLocalRef(jni_env, str_speaker_start_time);
    DETACH_JVM(jni_env);
}
#endif
#ifdef PJSIP_EXTEND_ZZY
extern jclass ptt_group_status_class;
static void update_ptt_state(multicast_INVITE *inv)
{
    jobject obj_jni_callback, obj_ptt_group_status;
    jmethodID mUpdatePttStatus, constructorMethodId;
    char str[GROUP_NUM_LENGTH];
    jstring str_group_num, speaker_num, str_start_time, str_speaker_start_time;
    jmethodID mSetGroupNum, mSetCallId, mSetSpeaker, mSetStartTime, mSetSpkStartTime, mSetStatus;
    JNIEnv *jni_env = 0;

    if (!jni_callback_class || !ptt_group_status_class)
        return;
    ATTACH_JVM(jni_env);

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "<init>", "()V");
    obj_ptt_group_status = (*jni_env)->NewObject(jni_env, ptt_group_status_class, constructorMethodId);

    pj_bzero(str, GROUP_NUM_LENGTH);
    pj_ansi_strncpy(str, inv->group_num.ptr, inv->group_num.slen);
    str_group_num = (*jni_env)->NewStringUTF(jni_env, str);
    mSetGroupNum = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setGroupNum", "(Ljava/lang/String;)V");
    mSetStatus = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setStatus", "(I)V");
    mSetSpeaker = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setSpeakerNum", "(Ljava/lang/String;)V");

    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetGroupNum, str_group_num);
    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetStatus, 1);
    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetSpeaker, NULL);

    mUpdatePttStatus = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "updatePttState", "(Lcom/zzy/ptt/model/PttGroupStatus;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mUpdatePttStatus, obj_ptt_group_status);

    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    (*jni_env)->DeleteLocalRef(jni_env, obj_ptt_group_status);
    (*jni_env)->DeleteLocalRef(jni_env, str_group_num);
    (*jni_env)->DeleteLocalRef(jni_env, speaker_num);
    (*jni_env)->DeleteLocalRef(jni_env, str_start_time);
    (*jni_env)->DeleteLocalRef(jni_env, str_speaker_start_time);
    DETACH_JVM(jni_env);
}

static void parse_reject_msg(char *msg_content, unsigned len)
{
    jobject obj_jni_callback, obj_ptt_group_status;
    jmethodID mUpdatePttStatus, constructorMethodId;
    jmethodID mSetStatus, mSetRejectReason;
    JNIEnv *jni_env = 0;

    pj_scanner scanner;
    pj_str_t out;
    char * tmp;

    if (!jni_callback_class || !ptt_group_status_class)
        return;
    ATTACH_JVM(jni_env);

    if(msg_content == NULL || len == 0)
    {
        return;
    }
    
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "<init>", "()V");
    obj_ptt_group_status = (*jni_env)->NewObject(jni_env, ptt_group_status_class, constructorMethodId);

    mSetStatus = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setStatus", "(I)V");
    mSetRejectReason = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setRejectReason", "(I)V");

    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetStatus, PTT_STATUS_REJECTED);
    PJ_LOG(3, (THIS_FILE, "parse_reject_msg %s", msg_content));
    pj_scan_init(&scanner, msg_content, len, 0, &on_scanner_error);

    while (!pj_scan_is_eof(&scanner))
    {
        if(!pj_scan_strcmp(&scanner, "rsp:ptt-reject", 14))
        {
            pj_scan_get_until_chr(&scanner,"\r\n", &out);
            pj_scan_get_newline(&scanner);
            //continue;
        }

        if(!pj_scan_strcmp(&scanner, "error:", 6))
        {
            pj_scan_get_until_ch(&scanner, ':', &out);
            pj_scan_get_char(&scanner);

            tmp = scanner.curptr;
            if(*(tmp++) == '\r' || *(tmp++) == '\n')
            {
                (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetRejectReason, -1);
                pj_scan_get_newline(&scanner);
                continue;
            }

            pj_scan_get_until_chr(&scanner, "\r\n", &out);
            PJ_LOG(3, (THIS_FILE, "error %.*s", out.slen,out.ptr));
            (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetRejectReason, pj_strtoul(&out));

            if(scanner.end > scanner.curptr)
                pj_scan_get_newline(&scanner);
            else
                continue;
        }
    }
    

    mUpdatePttStatus = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "updatePttState", "(Lcom/zzy/ptt/model/PttGroupStatus;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mUpdatePttStatus, obj_ptt_group_status);

    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    (*jni_env)->DeleteLocalRef(jni_env, obj_ptt_group_status);
    DETACH_JVM(jni_env);

}
static void parse_waiting_msg(char *msg_content, unsigned len)
{

    jobject obj_jni_callback, obj_ptt_group_status;
    jmethodID mUpdatePttStatus, constructorMethodId;
    jstring jstr_speaker;
    jmethodID mSetSpeaker, mSetStatus, mSetCurPos, mSetTotalWait;
    JNIEnv *jni_env = 0;
    char speaker[64] = {0};

	pj_scanner scanner;
	pj_str_t out;
	char * tmp;

	if (!jni_callback_class || !ptt_group_status_class)
        return;
    ATTACH_JVM(jni_env);

	if(msg_content == NULL || len == 0)
	{
		return;
	}
	
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "<init>", "()V");
    obj_ptt_group_status = (*jni_env)->NewObject(jni_env, ptt_group_status_class, constructorMethodId);

    mSetStatus = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setStatus", "(I)V");
    mSetSpeaker = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setSpeakerNum", "(Ljava/lang/String;)V");
    mSetCurPos = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setCurrentWaitingPos", "(I)V");
    mSetTotalWait = (*jni_env)->GetMethodID(jni_env, ptt_group_status_class, "setTotalWaitingCount", "(I)V");

    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetStatus, PTT_STATUS_WAITING);
	PJ_LOG(3, (THIS_FILE, "parse_waiting_msg %s", msg_content));
	pj_scan_init(&scanner, msg_content, len, 0, &on_scanner_error);

	while (!pj_scan_is_eof(&scanner))
	{
		if(!pj_scan_strcmp(&scanner, "rsp:ptt-waiting", 15))
		{
			pj_scan_get_until_chr(&scanner,"\r\n", &out);
			pj_scan_get_newline(&scanner);
			//continue;
			if(scanner.curptr == scanner.end)
			    break;
		}

		if(!pj_scan_strcmp(&scanner, "speaker:", 8))
		{
			pj_scan_get_until_ch(&scanner, ':', &out);
			pj_scan_get_char(&scanner);
			pj_bzero(speaker, pj_ansi_strlen(speaker));

			tmp = scanner.curptr;
			if(*(tmp++) == '\r' || *(tmp++) == '\n')
			{
			    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetSpeaker, NULL);
				pj_scan_get_newline(&scanner);
				//continue;
			}
			else
			{
                pj_scan_get_until_chr(&scanner, "\r\n", &out);
    			PJ_LOG(3, (THIS_FILE, "speaker %.*s", out.slen,out.ptr));
    			pj_ansi_strncpy(speaker, out.ptr, out.slen);
    			jstr_speaker = (*jni_env)->NewStringUTF(jni_env, speaker);
    			(*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetSpeaker, jstr_speaker);

    			if(scanner.end > scanner.curptr)
    				pj_scan_get_newline(&scanner);
    			else
    				break;
			}
		}
		if(!pj_scan_strcmp(&scanner, "queue:", 6))
		{
			pj_scan_get_until_ch(&scanner, ':', &out);
			pj_scan_get_char(&scanner);

			tmp = scanner.curptr;
			if(*(tmp++) == '\r' || *(tmp++) == '\n')
			{
			    (*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetTotalWait, -1);
				pj_scan_get_newline(&scanner);
				//continue;
				break;
			}

			pj_scan_get_until_ch(&scanner, '/', &out);
			PJ_LOG(3, (THIS_FILE, "current position %.*s", out.slen,out.ptr));
			(*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetCurPos, pj_strtoul(&out));

			pj_scan_get_char(&scanner);
			pj_scan_get_until_chr(&scanner, "\r\n", &out);
			PJ_LOG(3, (THIS_FILE, "total waiting count %.*s", out.slen,out.ptr));
			(*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetTotalWait, pj_strtoul(&out));
			if(scanner.end > scanner.curptr)
				pj_scan_get_newline(&scanner);
			else
				break;
		}
	}
    

    mUpdatePttStatus = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "updatePttState", "(Lcom/zzy/ptt/model/PttGroupStatus;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mUpdatePttStatus, obj_ptt_group_status);

    (*jni_env)->DeleteLocalRef(jni_env, obj_jni_callback);
    (*jni_env)->DeleteLocalRef(jni_env, obj_ptt_group_status);
    (*jni_env)->DeleteLocalRef(jni_env, jstr_speaker);
    DETACH_JVM(jni_env);
    
    return;
}


static void receive_ptt_right_response(ptt_status status, char *msg_content, unsigned len)
{
    jobject obj_jni_callback;
    jmethodID constructorMethodId, mReceivePttRightResponse;
    JNIEnv *jni_env = 0;

    PJ_LOG(3, (THIS_FILE, "receive_ptt_right_response status %d, msg_content %s", status, msg_content));

    switch (status)
	{
	case PTT_STATUS_BUSY:
	    //ptt_enter_state(PTT_STATUS_INIT);
		break;
	case PTT_STATUS_WAITING:
    	{
    	    //char *debug_msg = "rsp:ptt-reject\r\nerror:2\r\n";
    	    //ptt_enter_state(PTT_STATUS_WAITING);
    	    parse_waiting_msg(msg_content, len);
    	    //parse_waiting_msg(debug_msg, pj_ansi_strlen(debug_msg));

    	    //parse_reject_msg(debug_msg, pj_ansi_strlen(debug_msg));
    		return;
    	}
	case PTT_STATUS_CANCELED:
		break;
	case PTT_STATUS_FORBIDDEN:
		break;
	case PTT_STATUS_REJECTED:
	    {
            parse_reject_msg(msg_content, len);
		    return;
	    }
	default:
		PJ_LOG(3, (THIS_FILE, "receive_ptt_right_response The code should never run here!!!"));
		return;
	}

    if (!jni_callback_class)
        return;
    ATTACH_JVM(jni_env);

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    mReceivePttRightResponse = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "receivePttRightResponse", "(I)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mReceivePttRightResponse, status);
    DETACH_JVM(jni_env);
}

static void receive_member_state_change(multicast_MemberState *member_info)
{
    jobject obj_jni_callback, obj_member_info;
    jmethodID constructorMethodId, mReceiveMemberStateChange;
    JNIEnv *jni_env = 0;
    jmethodID setName, setNumber, setStatus;
    jstring strName, strNumber;
    char name[MEMBER_NAME_LENGTH];
    char number[MEMBER_NUM_LENGTH];

    if (!jni_callback_class)
        return;
    ATTACH_JVM(jni_env);
    PJ_LOG(3,(THIS_FILE,"   sreceive_member_state_change   "));
    if(member_info)
    {
        constructorMethodId = (*jni_env)->GetMethodID(jni_env, member_info_class, "<init>", "()V");
        obj_member_info = (*jni_env)->NewObject(jni_env, member_info_class, constructorMethodId);
        setName = (*jni_env)->GetMethodID(jni_env, member_info_class, "setName", "(Ljava/lang/String;)V");
        setNumber = (*jni_env)->GetMethodID(jni_env, member_info_class, "setNumber", "(Ljava/lang/String;)V");
        setStatus = (*jni_env)->GetMethodID(jni_env, member_info_class, "setStatus", "(I)V");

        pj_bzero(name, sizeof(name));
        pj_ansi_strncpy(name, member_info->name.ptr, member_info->name.slen);
        strName = (*jni_env)->NewStringUTF(jni_env, name);
        pj_bzero(number, sizeof(number));
        pj_ansi_strncpy(number, member_info->num.ptr, member_info->num.slen);
        strNumber = (*jni_env)->NewStringUTF(jni_env, number);

        (*jni_env)->CallVoidMethod(jni_env, obj_member_info, setName, strName);
        (*jni_env)->CallVoidMethod(jni_env, obj_member_info, setNumber, strNumber);
        (*jni_env)->CallVoidMethod(jni_env, obj_member_info, setStatus, (jint)(member_info->status));
    }
    else
    {
        obj_member_info = NULL;
    }

    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    mReceiveMemberStateChange = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "receiveMemberStateChange", "(Lcom/zzy/ptt/model/MemberInfo;)V");
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, mReceiveMemberStateChange, obj_member_info);
    DETACH_JVM(jni_env);
}


#endif


/*****************************************************************************
 * Public API
 */

/*
 * Parse reg200ok message body.
 */
int ptt_reg200ok_content_parse(char * content, unsigned len)
{
    int ichar;
    pj_scanner scanner;
    pj_str_t out;
    reg_content *group_info = &ptt_info.group_info;
    pj_pool_t *group_pool;

    if (group_info->signal_address.transport_id != PTT_INVALIDATE)
    {
        return PJ_SUCCESS;
    }

    pj_scan_init(&scanner, content, len, 0, &on_scanner_error);
    while (!pj_scan_is_eof(&scanner))
    {
        if(!pj_scan_strcmp(&scanner, "ipm-group:", 10))
        {
            pj_scan_get_until_ch(&scanner, ' ', &out);
            pj_scan_get_char(&scanner);
            group_pool = pjsua_pool_create("ptt%p", 256, 256);
            group_info->general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
            group_info->general_group->own_pool = group_pool;
            ptt_group_info_parse(&scanner, group_info->general_group);
            ichar = pj_scan_get_char(&scanner);
            while(ichar == ';')
            {
                ptt_group_info *general_group;

                group_pool = pjsua_pool_create("ptt%p", 256, 256);
                general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
                general_group->own_pool = group_pool;
                ptt_group_info_parse(&scanner, general_group);
                pj_list_push_back(group_info->general_group, general_group);
                ichar = pj_scan_get_char(&scanner);
            }
            if(ichar == '\r')
            {
                pj_scan_get_newline(&scanner);
            }
        }
        else if (!pj_scan_strcmp(&scanner, "ipm-emergency-call:", 19))
        {
            PJ_LOG(3, (THIS_FILE, "******emergency call group appear******"));//TODO:
            pj_scan_skip_line(&scanner);
        }
        else if(!pj_scan_strcmp(&scanner, "ipm-signal-address:", 19))
        {
            pj_scan_get_until_ch(&scanner, ' ', &out);
            pj_scan_get_char(&scanner);
            pj_scan_get_until_ch(&scanner, ':', &group_info->signal_address.mcast_address);
            pj_scan_get_char(&scanner);
            pj_scan_get_until_chr(&scanner, "\r\n", &out);
            group_info->signal_address.port = pj_strtoul(&out);
            pj_scan_get_newline(&scanner);
        }
        else
        {
            pj_scan_skip_line(&scanner);
        }
    }
#ifdef PJSIP_EXTEND_ZED
    receive_group_info(group_info->general_group);

    ptt_media_transport_create(group_info->general_group);
    ptt_transport_create(&group_info->signal_address.mcast_address, group_info->signal_address.port, &group_info->signal_address.transport_id);
#endif

    return PJ_SUCCESS;
}

#ifdef PJSIP_EXTEND_ZZY

static void cleanup_on_regroup(char *content, unsigned len)
{
    int ichar;
    pj_str_t out;
    pj_scanner scanner;
    ptt_group_info * node;
    pj_bool_t b_continue;
    ptt_group_info * node_erase = NULL;
    char *flag = NULL;
    
    node = ptt_info.group_info.general_group;
    PJ_LOG(3,(THIS_FILE, "cleanup_on_regroup content %s node %d", content, node));
    //pj_list_erase
    do
    {
        char group_num[GROUP_NUM_LENGTH] = {0};
        b_continue = PJ_FALSE;
        if (!node)
            break;

        pj_ansi_strncpy(group_num, node->number.ptr, node->number.slen);
        group_num[node->number.slen] = ',';
        node_erase = node;
        node = node->next;
        flag = pj_ansi_strstr(content, group_num);
        PJ_LOG(3, (THIS_FILE, "cleanup_on_regroup flag %s", flag));
        if (!flag)
        {
            if (ptt_state > PTT_STATUS_UNINIT && !pj_strcmp(&g_current_group->number, &node_erase->number))
            {
                ptt_close_media();
                pj_bzero(&call_info, sizeof(multicast_INVITE));
                pj_bzero(current_call_id, 512);
                pj_bzero(g_speaker, 512);
                ptt_enter_state(PTT_STATUS_UNINIT);
                g_current_group = NULL;
            }
            if (node_erase == ptt_info.group_info.general_group)
                ptt_info.group_info.general_group = node;
            pj_list_erase(node_erase);
            ptt_media_transport_destroy(node_erase, PJ_FALSE);
        }
        else
        {
            node_erase = NULL;
        }

        if (node == ptt_info.group_info.general_group && node != node_erase && node_erase != NULL)
            b_continue = PJ_TRUE;

    } while (node != ptt_info.group_info.general_group || b_continue);

    if(node == node_erase)
        ptt_info.group_info.general_group = NULL;
    node = ptt_info.group_info.general_group;
    if (!node)
    {
        PJ_LOG(3,(THIS_FILE, "free_group_memory group_info and reinit group info"));
        group_info_destroy(&ptt_info.group_info);
        pj_bzero(&ptt_info, sizeof(PTT_info));
        group_info_init(&ptt_info.group_info);
        ptt_receive_group_info_parse(content, len, PJ_FALSE);
        return;
    }

    //pj_list_push_back
    pj_scan_init(&scanner, content, len, 0, &on_scanner_error);
    while (!pj_scan_is_eof(&scanner))
    {
        if(!pj_scan_strcmp(&scanner, "general-group:", 14))
        {
            pj_pool_t *group_pool;
            pj_bool_t bfind = PJ_FALSE;
            ptt_group_info *general_group;

            pj_scan_get_until_ch(&scanner, ':', &out);
            pj_scan_get_char(&scanner);
            group_pool = pjsua_pool_create("ptt%p", 256, 256);
            general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
            general_group->own_pool = group_pool;
            ptt_group_info_parse(&scanner, general_group);
            do 
            {
                if (!pj_strcmp(&node->number, &general_group->number))
                {
                    bfind = PJ_TRUE;
                    break;
                }
                node = node->next;

            } while (node != ptt_info.group_info.general_group);
            if (bfind)
            {
                pj_pool_release(group_pool);
                group_pool = NULL;
                general_group = NULL;
            }
            else
            {
                ptt_media_transport_create(general_group);
                pj_list_push_back(ptt_info.group_info.general_group, general_group);
            }

            ichar = pj_scan_get_char(&scanner);
            while(ichar == ';')
            {
                bfind = PJ_FALSE;
                node= ptt_info.group_info.general_group;

                group_pool = pjsua_pool_create("ptt%p", 256, 256);
                general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
                general_group->own_pool = group_pool;
                ptt_group_info_parse(&scanner, general_group);
                do
                {
                    if (!pj_strcmp(&node->number, &general_group->number))
                    {
                        bfind = PJ_TRUE;
                        break;
                    }
                    node = node->next;

                } while (node != ptt_info.group_info.general_group);
                if (bfind)
                {
                    pj_pool_release(group_pool);
                    group_pool = NULL;
                    general_group = NULL;
                }
                else
                {
                    ptt_media_transport_create(general_group);
                    pj_list_push_back(ptt_info.group_info.general_group, general_group);
                }

                ichar = pj_scan_get_char(&scanner);
            }
            if(ichar == '\r')
            {
                pj_scan_get_newline(&scanner);
            }
        }
        else
        {
            pj_scan_skip_line(&scanner);
        }
    }

    receive_group_info(ptt_info.group_info.general_group);
}

int ptt_receive_group_info_parse(char *content, unsigned len, pj_bool_t b_regroup)
{
    int ichar;
    pj_scanner scanner;
    pj_str_t out;
    reg_content *group_info = &ptt_info.group_info;
    pj_pool_t *group_pool;

    pj_str_t fake_addr;
    char * tmp;

    if(b_regroup)
    {
        cleanup_on_regroup(content, len);
        return PJ_SUCCESS;
    }
    else
    {
        if(group_info->signal_address.transport_id != PTT_INVALIDATE)
        {
            receive_group_info(group_info->general_group);
            return PJ_SUCCESS;
        }
    }
    
    pj_scan_init(&scanner, content, len, 0, &on_scanner_error);
    while (!pj_scan_is_eof(&scanner))
    {
        if(!pj_scan_strcmp(&scanner, "general-group:", 14))
        {
            pj_scan_get_until_ch(&scanner, ':', &out);
            pj_scan_get_char(&scanner);
            tmp = scanner.curptr;
            if(*(tmp++) == '\r' || *(tmp++) == '\n')
            {
                pj_scan_get_newline(&scanner);
                continue;
            }

            group_pool = pjsua_pool_create("ptt%p", 256, 256);
            group_info->general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
            group_info->general_group->own_pool = group_pool;
            ptt_group_info_parse(&scanner, group_info->general_group);
            ichar = pj_scan_get_char(&scanner);

            while(ichar == ';')
            {
                ptt_group_info *general_group;

                group_pool = pjsua_pool_create("ptt%p", 256, 256);
                general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
                general_group->own_pool = group_pool;
                ptt_group_info_parse(&scanner, general_group);
                pj_list_push_back(group_info->general_group, general_group);
                ichar = pj_scan_get_char(&scanner);
            }
            if(ichar == '\r' || ichar == '\n')
            {
                pj_scan_get_newline(&scanner);
                pj_scan_strcmp(&scanner, "emergency-group:", 16);
            }
        }
        else if (!pj_scan_strcmp(&scanner, "emergency-group:", 16))
        {
            //TODO:
            pj_scan_skip_line(&scanner);
        }
        else if(!pj_scan_strcmp(&scanner, "signal-address:", 15))
        {
            //TODO:
            pj_scan_get_char(&scanner);
            pj_scan_get_until_ch(&scanner, ':', &out);
            pj_scan_get_char(&scanner);
            pj_scan_get_until_ch(&scanner, ':', &group_info->signal_address.mcast_address);
            pj_scan_get_char(&scanner);
            pj_scan_get_until_chr(&scanner, "\r\n", &out);
            group_info->signal_address.port = pj_strtoul(&out);
            pj_scan_get_newline(&scanner);
        }
        else
        {
            pj_scan_skip_line(&scanner);
        }
    }

    receive_group_info(group_info->general_group);

    //lxd commented
    //ptt_transport_create(&group_info->signal_address.mcast_address, group_info->signal_address.port, &group_info->signal_address.transport_id);
    //ptt_media_transport_create(group_info->general_group);

    return PJ_SUCCESS;

}



#endif

/*
 * Parse info200ok message body.
 */
int ptt_info200ok_content_parse(char * content, unsigned len)
{
    pj_pool_t *tmp_pool;
    pj_scanner scanner;
    int ichar;
    pj_str_t out;
    char group_num[GROUP_NUM_LENGTH] = {0};
    ptt_member_info member_list = {0};
    int real_len = len - 2;

    if (!content && len == PJSIP_SC_FORBIDDEN)
    {
        receive_ptt_right_response(PTT_STATUS_FORBIDDEN, content, len);
        return PJ_SUCCESS;
    }
    
#ifdef PJSIP_EXTEND_ZED
    pj_scan_init(&scanner, content, len, 0, &on_scanner_error);
    while (!pj_scan_is_eof(&scanner))
    {
        if((real_len >= 18)&&(!pj_scan_strcmp(&scanner, "ipm-req: getstatus", 18)))
        {
            pj_scan_get_until_ch(&scanner, ' ', &out);
            pj_scan_get_char(&scanner);
            pj_scan_get_until_ch(&scanner, ' ', &out);
            if (!*scanner.curptr)
            {
                pjsua_perror(THIS_FILE, "NO group number", *scanner.curptr);
                return PJ_SUCCESS;
            }
            tmp_pool = pjsua_pool_create("tmp", 512, 512);
            pj_list_init(&member_list);
            pj_scan_get_char(&scanner);
            pj_scan_get_until_ch(&scanner, '(', &out);//group number
            pj_ansi_strncpy(group_num, out.ptr, out.slen);
            ichar = pj_scan_get_char(&scanner);
            ptt_member_info_parse(&scanner, &member_list);
            ichar = *(scanner.curptr);
            while(ichar != ')')
            {
                ptt_member_info *member_info;

                ichar = pj_scan_get_char(&scanner);
                member_info = pj_pool_alloc(tmp_pool, sizeof(ptt_member_info));
                ptt_member_info_parse(&scanner, member_info);
                pj_list_push_back(&member_list, member_info);
                ichar = *(scanner.curptr);
            }
            pj_scan_get_char(&scanner);
            ichar = pj_scan_get_char(&scanner);
            if(ichar == '\r')
            {
                pj_scan_get_newline(&scanner);
            }
            receive_member_info(&member_list, group_num);
            pj_pool_release(tmp_pool);
        }
        else if((real_len >= 15) && (!pj_scan_strcmp(&scanner, "ipm-req: accept", 15)))
        {
            receive_ptt_right_response(PTT_STATUS_BUSY);
            break;
        }
        else if((real_len >= 16) && (!pj_scan_strcmp(&scanner, "ipm-req: waiting", 16)))
        {
            receive_ptt_right_response(PTT_STATUS_WAITING);
            break;
        }
        else if((real_len >= 15) && (!pj_scan_strcmp(&scanner, "ipm-req: reject", 15)))
        {
            receive_ptt_right_response(PTT_STATUS_REJECTED);
            break;
        }
        else if((real_len >= 15) && (!pj_scan_strcmp(&scanner, "ipm-req: cancel", 15)))
        {
            receive_ptt_right_response(PTT_STATUS_CANCELED);
            break;
        }
        else
        {
            pj_scan_skip_line(&scanner);
        }
    }
#endif
    return PJ_SUCCESS;
}

/*
 * Parse Message message body.
 */
int ptt_message_content_parse(char * content, unsigned len)
{
    int ichar;
    pj_str_t out;
    pj_scanner scanner;
    ptt_group_info * node;
    pj_bool_t b_continue;
    ptt_group_info * node_erase = NULL;

    node = ptt_info.group_info.general_group;
    //pj_list_erase
    do
    {
        char group_num[GROUP_NUM_LENGTH] = {0};
        b_continue = PJ_FALSE;
        if (!node)
            break;

        pj_ansi_strncpy(group_num, node->number.ptr, node->number.slen);
        node_erase = node;
        node = node->next;
        if (!pj_ansi_strstr(content, group_num))
        {
            if (ptt_state > PTT_STATUS_UNINIT && !pj_strcmp(&g_current_group->number, &node_erase->number))
            {
                ptt_close_media();
                pj_bzero(&call_info, sizeof(multicast_INVITE));
                ptt_enter_state(PTT_STATUS_UNINIT);
                g_current_group = NULL;
            }
            if (node_erase == ptt_info.group_info.general_group)
                ptt_info.group_info.general_group = node;
            pj_list_erase(node_erase);
            ptt_media_transport_destroy(node_erase, PJ_FALSE);
        }
        else
        {
            node_erase = NULL;
        }

        if (node == ptt_info.group_info.general_group && node != node_erase && node_erase != NULL)
            b_continue = PJ_TRUE;

    } while (node != ptt_info.group_info.general_group || b_continue);

    if(node == node_erase)
        ptt_info.group_info.general_group = NULL;
    node = ptt_info.group_info.general_group;
    if (!node)
    {
        group_info_destroy(&ptt_info.group_info);
        pj_bzero(&ptt_info, sizeof(PTT_info));
        group_info_init(&ptt_info.group_info);
        ptt_reg200ok_content_parse(content, len);

        return PJ_SUCCESS;
    }

    //pj_list_push_back
    pj_scan_init(&scanner, content, len, 0, &on_scanner_error);
    while (!pj_scan_is_eof(&scanner))
    {
        if(!pj_scan_strcmp(&scanner, "ipm-group:", 10))
        {
            pj_pool_t *group_pool;
            pj_bool_t bfind = PJ_FALSE;
            ptt_group_info *general_group;

            pj_scan_get_until_ch(&scanner, ' ', &out);
            pj_scan_get_char(&scanner);
            group_pool = pjsua_pool_create("ptt%p", 256, 256);
            general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
            general_group->own_pool = group_pool;
            ptt_group_info_parse(&scanner, general_group);
            do 
            {
                if (!pj_strcmp(&node->number, &general_group->number))
                {
                    bfind = PJ_TRUE;
                    break;
                }
                node = node->next;

            } while (node != ptt_info.group_info.general_group);
            if (bfind)
            {
                pj_pool_release(group_pool);
                group_pool = NULL;
                general_group = NULL;
            }
            else
            {
                ptt_media_transport_create(general_group);
                pj_list_push_back(ptt_info.group_info.general_group, general_group);
            }

            ichar = pj_scan_get_char(&scanner);
            while(ichar == ';')
            {
                bfind = PJ_FALSE;
                node= ptt_info.group_info.general_group;

                group_pool = pjsua_pool_create("ptt%p", 256, 256);
                general_group = pj_pool_alloc(group_pool, sizeof(ptt_group_info));
                general_group->own_pool = group_pool;
                ptt_group_info_parse(&scanner, general_group);
                do
                {
                    if (!pj_strcmp(&node->number, &general_group->number))
                    {
                        bfind = PJ_TRUE;
                        break;
                    }
                    node = node->next;

                } while (node != ptt_info.group_info.general_group);
                if (bfind)
                {
                    pj_pool_release(group_pool);
                    group_pool = NULL;
                    general_group = NULL;
                }
                else
                {
                    ptt_media_transport_create(general_group);
                    pj_list_push_back(ptt_info.group_info.general_group, general_group);
                }

                ichar = pj_scan_get_char(&scanner);
            }
            if(ichar == '\r')
            {
                pj_scan_get_newline(&scanner);
            }
        }
        else
        {
            pj_scan_skip_line(&scanner);
        }
    }

    receive_group_info(ptt_info.group_info.general_group);

    return PJ_SUCCESS;
}

pj_status_t manage_ptt_group(pj_uint16_t optname)
{
    pj_status_t status = PJ_SUCCESS;
    pjsua_transport_id signal_transport_id;
    ptt_group_info* media_group, * node;

    signal_transport_id = ptt_info.group_info.signal_address.transport_id;
    if (signal_transport_id != PTT_INVALIDATE)
    {
        status = manage_signal_group(pjsua_get_transport(signal_transport_id), optname);
        if (status)
        {
            PJ_LOG(4,(THIS_FILE, "SO_BINDTODEVICE signal igmp error %d!", status));
        }
    }

    media_group = ptt_info.group_info.general_group;
    node = media_group;
    do
    {PJ_LOG(4,(THIS_FILE, "media loop!"));
        if (node->transport)
        {PJ_LOG(4,(THIS_FILE, "manage_media_group!"));
            status = manage_media_group(node, optname);
            if(status)
            {
                PJ_LOG(4,(THIS_FILE, "SO_BINDTODEVICE media igmp error %d!", status));
            }
        }
        node = node->next;
    } while (node != media_group);

    return status;
}

ptt_status ptt_get_state(void)
{
    return ptt_state;
}

pj_bool_t group_number_compare(char *number)
{
    pj_str_t num = pj_str(number);

    return multicast_info_compare(g_current_group, &num);
}

/*
 * only called by jni when single call exist
 */
pj_status_t ptt_open_media(void)
{
    pj_status_t status;

    PJ_LOG(3,(THIS_FILE,">>>>>>>>>>>>>>>>>>>>>ptt_open_media ptt_state %d<<<<<<<<<<<<<<<<<<", ptt_state));
    if (ptt_state != PTT_STATUS_INIT)
    {
        if(ptt_state == PTT_STATUS_FREE)
        {
            if(stream)
                pjmedia_stream_resume(stream, PJMEDIA_DIR_PLAYBACK);
            pjsua_call_hangup_all();
            ptt_enter_state(PTT_STATUS_BUSY);
            return;
        }
        else
            return PJ_EINVALIDOP;
    }
        
    pjsua_call_hangup_all();
    status = multicast_call_incoming(g_current_group, call_info.codec);
    if (status != PJ_SUCCESS)
        return status;
    ptt_enter_state(PTT_STATUS_BUSY);

    return PJ_SUCCESS;
}

pj_status_t ptt_close_media(void)
{
    PJ_LOG(3,(THIS_FILE,">>>>>>>>>>>>>>>>>>>>>ptt_close_media<<<<<<<<<<<ptt_state %d", ptt_state)); 
    multicast_call_ending(g_current_group);
    if (ptt_state > PTT_STATUS_UNINIT)
        ptt_enter_state(PTT_STATUS_INIT);

    return PJ_SUCCESS;
}

/*
 * init
 * set global default value
 */
int ptt_init(void)
{
    pj_status_t status;

    group_info_init(&ptt_info.group_info);

    /* Initialize our module to handle otherwise unhandled request */
    status = pjsip_endpt_register_module(pjsua_get_pjsip_endpt(), &mod_multicast_handler);
    if (status != PJ_SUCCESS)
        return status;

    return PJ_SUCCESS;
}

/*
 * release memory
 * clear global value
 */
int ptt_destroy(void)
{
    PJ_LOG(3, (THIS_FILE, ">>>>ptt_destroy"));
    if (mod_multicast_handler.id != -1)
    {
        pjsip_endpt_unregister_module(pjsua_get_pjsip_endpt(), &mod_multicast_handler);
    }
    group_info_destroy(&ptt_info.group_info);

    pj_bzero(&ptt_info, sizeof(PTT_info));
    ptt_info.group_info.signal_address.transport_id = PTT_INVALIDATE;

    return PJ_SUCCESS;
}

#ifdef PJ_ANDROID
static void jni_cb_receive_msg(const char *number, const char *body_text)
{
    jmethodID rcvMsgId, constructorMethodId;
    jstring stringNumber = NULL, stringText = NULL;
    jobject obj_jni_callback;
    JNIEnv *jni_env = 0;
    //char from_number[32] = {0};

    if (!jni_callback_class)
        return;

    PJ_LOG(3, (THIS_FILE, "jni_cb_receive_msg"));
/*
    char *p1 = pj_ansi_strchr(from->ptr, ':');
    char *p2 = pj_ansi_strchr(from->ptr, '@');

    if (!jni_callback_class)
        return;

    if (p1 && p2)
    {
        p1+=1;
        while(*p1 == ' ') p1++;
        pj_ansi_sprintf(from_number, "%.*s", p2-p1, p1);
        PJ_LOG(3, (THIS_FILE, "from:%s", from_number));
    }
*/
    ATTACH_JVM(jni_env);
    constructorMethodId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "<init>", "()V");
    obj_jni_callback = (*jni_env)->NewObject(jni_env, jni_callback_class, constructorMethodId);
    rcvMsgId = (*jni_env)->GetMethodID(jni_env, jni_callback_class, "receiveMessage", "(Ljava/lang/String;Ljava/lang/String;)V");
/*
    if (b_group_change)
    {
        (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, rcvMsgId, NULL, NULL);//reuse to indicate group changed
        ptt_message_content_parse(body->ptr, body->slen);
    }
    else
    {
        
    }
*/
    stringNumber = (*jni_env)->NewStringUTF(jni_env, number);
    stringText = (*jni_env)->NewStringUTF(jni_env, body_text);
    (*jni_env)->CallVoidMethod(jni_env, obj_jni_callback, rcvMsgId, stringNumber, stringText);

    
    DETACH_JVM(jni_env);
}
#endif

#ifdef PJSIP_EXTEND_ZZY
void do_receive_sms(const pj_str_t *from, const pj_str_t *body)
{
    char *p1 = pj_ansi_strchr(from->ptr, ':');
    char *p2 = pj_ansi_strchr(from->ptr, '@');
    char from_number[32] = {0};

    PJ_LOG(3, (THIS_FILE, "do_receive_sms from %.*s, body : %.*s", 
                from->slen, from->ptr,
                body->slen, body->ptr));

    if (p1 && p2)
    {
        p1+=1;
        while(*p1 == ' ') p1++;
        pj_ansi_sprintf(from_number, "%.*s", p2-p1, p1);
        PJ_LOG(3, (THIS_FILE, "from:%s", from_number));
    }

    jni_cb_receive_msg(from_number, body->ptr);
}

 void do_receive_group_info(const char *msg_content, unsigned len, pj_bool_t b_regroup)
{
    PJ_LOG(3, (THIS_FILE, "do_receive_group_info, content : %s", msg_content));
    ptt_receive_group_info_parse(msg_content, len, b_regroup);//just reuse this function
    //PJ_LOG(3, (THIS_FILE, "do_receive_group_info, content : %s", msg_content));
}

 //lxd added for 3G
void do_receive_minvite_by_smessage(const char *msg_content, unsigned len)
{
	pj_scanner scanner;
	pj_str_t out;
	char speaker[64] = {0};
	char group[64] = {0};
	pj_str_t pj_group;
	pj_str_t pj_speaker;
	char * tmp;
	pj_bool_t bFind = PJ_TRUE;

	pj_scan_init(&scanner, msg_content, len, 0, &on_scanner_error);
	PJ_LOG(3, (THIS_FILE, "sxsexe do_receive_minvite_by_smessage, content : %s", msg_content));

	while (!pj_scan_is_eof(&scanner))
	{
		if(!pj_scan_strcmp(&scanner, "ind:call-info", 13))
		{
			PJ_LOG(3, (THIS_FILE, "sxsexe 1111111111111111111111"));
			pj_scan_get_until_chr(&scanner,"\r\n", &out);
			pj_scan_get_newline(&scanner);
			//continue;
			if(scanner.curptr == scanner.end)
			{
				bFind = PJ_FALSE;
				break;
			}
		}

		if(!pj_scan_strcmp(&scanner, "group:", 6))
		{
			PJ_LOG(3, (THIS_FILE, "sxsexe 22222222222222222"));
			pj_scan_get_until_ch(&scanner, ':', &out);
			pj_scan_get_char(&scanner);
			pj_bzero(speaker, pj_ansi_strlen(speaker));

			tmp = scanner.curptr;
			if(*(tmp++) == '\r' || *(tmp++) == '\n')
			{
				//(*jni_env)->CallVoidMethod(jni_env, obj_ptt_group_status, mSetSpeaker, NULL);
				pj_scan_get_newline(&scanner);
				bFind = PJ_FALSE;
				break;
				//continue;
			}
			else
			{
				pj_scan_get_until_chr(&scanner, "\r\n", &out);
				PJ_LOG(3, (THIS_FILE, "sxsexe   group %.*s", out.slen,out.ptr));
				pj_ansi_strncpy(group, out.ptr, out.slen);
				pj_group = pj_str(group);
				if(scanner.end > scanner.curptr)
					pj_scan_get_newline(&scanner);
				else
					break;
			}
		}

		if(!pj_scan_strcmp(&scanner, "speaker:", 8))
		{
			PJ_LOG(3, (THIS_FILE, "sxsexe 33333333333333333333333333"));
			pj_scan_get_until_ch(&scanner, ':', &out);
			pj_scan_get_char(&scanner);

			tmp = scanner.curptr;
			if(*(tmp++) == '\r' || *(tmp++) == '\n')
			{
				pj_scan_get_newline(&scanner);
				break;
			}

			pj_scan_get_until_chr(&scanner, "\r\n", &out);
			PJ_LOG(3, (THIS_FILE, "sxsexe  speaker %.*s", out.slen,out.ptr));
			pj_ansi_strncpy(speaker, out.ptr, out.slen);
			pj_speaker = pj_str(speaker);
			if(scanner.end > scanner.curptr)
				pj_scan_get_newline(&scanner);
			else
				break;
		}

		if(!pj_scan_strcmp(&scanner, "call-id:", 8))
		{
			break;
		}
	}

	PJ_LOG(3, (THIS_FILE, "sxsexe pj_group %.*s pj_speaker %.*s bFind %d", pj_group.slen, pj_group.ptr, pj_speaker.slen, pj_speaker.ptr, bFind));
	if(bFind) {
		multicast_invite_indicate(&pj_group, &pj_speaker);
	}

}

void do_receive_member_info(const char *msg_content, unsigned len)
{
    pj_scanner scanner;
    pj_str_t out;
    int ichar;
    pj_pool_t *tmp_pool;
    char group_num[GROUP_NUM_LENGTH] = {0};
    ptt_member_info member_info = {0};
    ptt_member_info member_list = {0};

    PJ_LOG(3, (THIS_FILE, "do_receive_member_info, content : %s", msg_content));

    pj_scan_init(&scanner, msg_content, len, 0, &on_scanner_error);

    while (!pj_scan_is_eof(&scanner))
    {
        tmp_pool = pjsua_pool_create("tmp", 512, 512);
        //pj_memset(&member_info, 0, sizeof(ptt_member_info));
        pj_list_init(&member_list);
        //pj_scan_get_char(&scanner);
        pj_scan_get_until_ch(&scanner, '(', &out);//group number
        pj_ansi_strncpy(group_num, out.ptr, out.slen);
       // PJ_LOG(3,(THIS_FILE, "1111 group_num : %s, scanner : %s", group_num, scanner.curptr));
        pj_scan_get_char(&scanner);
        ptt_member_info_parse(&scanner, &member_list);
        //PJ_LOG(3,(THIS_FILE, "2222 scanner : %s,ichar : %c", scanner.curptr, ichar));
        ichar = *(scanner.curptr);
        while(ichar!= ')')
        {
            ptt_member_info *member_info;

            //PJ_LOG(3,(THIS_FILE, "3333 scanner : %s,ichar : %c", scanner.curptr, ichar));
            ichar = pj_scan_get_char(&scanner);
            //PJ_LOG(3,(THIS_FILE, "4444 scanner : %s,ichar : %c", scanner.curptr, ichar));
            member_info = pj_pool_alloc(tmp_pool, sizeof(ptt_member_info));
            ptt_member_info_parse(&scanner, member_info);
            pj_list_push_back(&member_list, member_info);
            ichar = *(scanner.curptr);
        }

        pj_scan_get_char(&scanner);
        ichar = pj_scan_get_char(&scanner);
        //PJ_LOG(3,(THIS_FILE, "6666 ichar : %c", ichar));
        if(ichar == '\r' || ichar == '\n')
        {
            pj_scan_get_newline(&scanner);
        }
        //PJ_LOG(3,(THIS_FILE, "5555 scanner : %s, (end-cur):%d", scanner.curptr, (scanner.end-scanner.curptr)));
    }
    receive_member_info(&member_list, group_num);
    pj_pool_release(tmp_pool);
}

void do_receive_regroupInfo(const char *msg_content, unsigned len)
{
    jni_cb_receive_msg(NULL, NULL);
    do_receive_group_info(msg_content, len, PJ_TRUE);//resue this function
}

void do_receive_ptt_rsp(ptt_status status, char *msg_content, unsigned len)
{
    receive_ptt_right_response(status, msg_content, len);
}

void check_ptt_error(unsigned ptt_err_no)
{
    PJ_LOG(3,(THIS_FILE, "check_ptt_error ptt_state %d", ptt_state));
    if(ptt_err_no == 1)
    {
        if (ptt_state != PTT_STATUS_UNINIT)
        {
            ptt_close_media();
            pj_bzero(&call_info, sizeof(multicast_INVITE));
            pj_bzero(current_call_id, 512);
            pj_bzero(g_speaker, 512);
            ptt_enter_state(PTT_STATUS_UNINIT);
            g_current_group = NULL;
        }
        multicast_bye_indicate();
    }
}

#endif



