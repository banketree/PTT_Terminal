/**************************************************************
* Copyright (C), 2012-201x, ZZY Tech. Co., Ltd.
* Project:   ZZY PTT 1.0
* File name: multicast_parser.c
* Author:    ZhangZhiChao
* Version:   v1.0
* Date:      2012.04
* Description: ptt multicast communication relation interface
**************************************************************/

#include <pjsip/sip_transport.h>        /* rdata structure */
#include <pj/except.h>
#include <pj/log.h>
#include <pj/string.h>

enum
{
    PTT_SUCCESS,
    SYNTAX_ERROR = 1
};

#define THIS_FILE	"multicast_parser.c"

static multicast_signal multicast_info;

static void on_scanner_error(pj_scanner *scanner)
{
    PJ_UNUSED_ARG(scanner);

    PJ_THROW(SYNTAX_ERROR);
}

#ifdef PJSIP_EXTEND_ZED
static void multicast_parse_info(pj_scanner *scanner, multicast_INFO *info)
{
    pj_str_t out;

    pj_scan_get_until_chr(scanner, "\r\n", &info->group_num);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &out);
    info->call_id = pj_strtoul(&out);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &info->start_time);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &out);
    info->status = pj_strtoul(&out);
    pj_scan_get_newline(scanner);
    if (info->status == 2)
    {
        pj_scan_get_until_ch(scanner, ' ', &out);
        pj_scan_get_char(scanner);
        pj_scan_get_until_chr(scanner, "\r\n", &info->speaker);
        pj_scan_get_newline(scanner);
        pj_scan_get_until_ch(scanner, ' ', &out);
        pj_scan_get_char(scanner);
        pj_scan_get_until_chr(scanner, "\r\n", &info->spk_start_time);
        pj_scan_get_newline(scanner);
    }
}

static void multicast_parse_invite(pj_scanner *scanner, multicast_INVITE *inv)
{
    pj_str_t out;

    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &inv->group_num);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &inv->speaker);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &out);
    inv->call_id = pj_strtoul(&out);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &inv->start_time);
    pj_scan_get_newline(scanner);
    if(!pj_scan_is_eof(scanner))
    {
        pj_scan_get_until_ch(scanner, ' ', &out);
        pj_scan_get_char(scanner);
        pj_scan_get_until_chr(scanner, "\r\n", &out);
        inv->codec = pj_strtoul(&out);
        pj_scan_get_newline(scanner);
    }
    else
    {
        inv->codec = 0xff;
    }
}

static void multicast_parse_bye(pj_scanner *scanner, multicast_BYE *bye)
{
    pj_str_t out;

    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &bye->group_num);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &out);
    bye->call_id = pj_strtoul(&out);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ' ', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &bye->start_time);
    pj_scan_get_newline(scanner);
}

/*****************************************************************************
 * Public API
 */

/*
 * parse multicast data.
 */
int multicast_parse_rdata(char *buf, pj_size_t size, pjsip_rx_data *rdata)
{
    pj_scanner scanner;
    pj_str_t out;

    pj_scan_init(&scanner, buf, size, 0, &on_scanner_error);
    while (!pj_scan_is_eof(&scanner))
    {
        if (!pj_scan_strcmp(&scanner, "INFO", 4))
        {
            pj_scan_get_until_ch(&scanner, ' ', &out);
            pj_scan_get_char(&scanner);
            if (!pj_scan_strcmp(&scanner, "ServerHeartbeat", 15))
            {
                pj_scan_skip_line(&scanner);
                pj_scan_get_until_ch(&scanner, ' ', &out);
                pj_scan_get_char(&scanner);
                pj_scan_get_until_chr(&scanner, "\r\n", &out);
                multicast_info.type = TYPE_HEARTBEAT;
                multicast_info.multi.heart_beat = pj_strtoul(&out);
                //PJ_LOG(3, (THIS_FILE, "ServerHeartbeat: %d", multicast_info.heart_beat));
                pj_scan_get_newline(&scanner);
            }
            else
            {
                multicast_INFO *info = &multicast_info.multi.info;

                multicast_info.type = TYPE_INFO;
                multicast_parse_info(&scanner, info);
            }
        }
        else if (!pj_scan_strcmp(&scanner, "INVITE", 6))
        {
            multicast_INVITE *inv = &multicast_info.multi.inv;

            multicast_info.type = TYPE_INVITE;
            multicast_parse_invite(&scanner, inv);
        }
        else if (!pj_scan_strcmp(&scanner, "BYE", 3))
        {
            multicast_BYE *bye = &multicast_info.multi.bye;

            multicast_info.type = TYPE_BYE;
            multicast_parse_bye(&scanner, bye);
        }
        else
        {
            pj_scan_skip_line(&scanner);
        }
    }

    rdata->multicast_info = multicast_info;

    return PJ_SUCCESS;
}
#endif

#ifdef PJSIP_EXTEND_ZZY

static void multicast_parse_memberstatus(pj_scanner *scanner, multicast_MemberState *member_info)
{
    pj_str_t out;
    pj_scan_get_until_ch(scanner, ',', &member_info->num);//member number
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ',', &member_info->name);//member name
    pj_scan_get_char(scanner);
    pj_scan_get_until_ch(scanner, ';', &out);//member state
    pj_scan_get_char(scanner);
    member_info->status = pj_strtoul(&out);
}

static void multicast_parse_invite(pj_scanner *scanner, multicast_INVITE *inv)
{
    pj_str_t out;
    pj_scan_get_until_ch(scanner, ':', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &inv->group_num);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ':', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &inv->speaker);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ':', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &inv->call_id);

    inv->codec = 0xff;//TODO
}


static void multicast_parse_bye(pj_scanner *scanner, multicast_BYE *bye)
{
    pj_str_t out;

    pj_scan_get_until_ch(scanner, ':', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &bye->group_num);
    pj_scan_get_newline(scanner);
    pj_scan_get_until_ch(scanner, ':', &out);
    pj_scan_get_char(scanner);
    pj_scan_get_until_chr(scanner, "\r\n", &bye->call_id);
}

static void multicast_parse_heartbeat(pj_scanner *scanner, heart_beat_group_info *heart_group)
{
    pj_scan_get_until_ch(scanner, ',', &heart_group->group_num);
    heart_group->prev = heart_group->next = heart_group;
}

/*****************************************************************************
 * Public API
 */

/*
 * parse multicast data.
 */
int multicast_parse_rdata(char *buf, pj_size_t size, pjsip_rx_data *rdata)
{
    pj_scanner scanner;
    pj_str_t out;

    pj_scan_init(&scanner, buf, size, 0, &on_scanner_error);
    
    while (!pj_scan_is_eof(&scanner))
    {
        if (!pj_scan_strcmp(&scanner, "ind:", 4))
        {
            pj_scan_get_until_ch(&scanner, ':', &out);
            pj_scan_get_char(&scanner);
            if (!pj_scan_strcmp(&scanner, "call-info", 9))
            {
                multicast_INVITE *inv = &multicast_info.multi.inv;
                multicast_info.type = TYPE_INVITE;
                pj_scan_get_until_chr(&scanner, "\r\n", &out);
                pj_scan_get_newline(&scanner);
                multicast_parse_invite(&scanner, inv);
                break;
            }
            else if (!pj_scan_strcmp(&scanner, "close", 5))
            {
                multicast_BYE *bye = &multicast_info.multi.bye;
                multicast_info.type = TYPE_BYE;
                pj_scan_get_until_chr(&scanner, "\r\n", &out);
                pj_scan_get_newline(&scanner);
                multicast_parse_bye(&scanner, bye);
                break;
            }
            else if (!pj_scan_strcmp(&scanner, "member-statuschange", 19))
            {
                multicast_MemberState *member_state = &multicast_info.multi.member_state;
                multicast_info.type = TYPE_MEMBER_STATE;
                pj_scan_get_until_chr(&scanner, "\r\n", &out);
                pj_scan_get_newline(&scanner);
                multicast_parse_memberstatus(&scanner, member_state);
                break;
            }
            
        }
        else if (!pj_scan_strcmp(&scanner, "heartbeat:", 10))
        {
            multicast_HEART *heart = NULL;
            heart_beat_group_info group_info = {0};
            pj_pool_t *group_pool;
            int ichar;

            heart = &multicast_info.multi.heart_beat;
            multicast_info.type = TYPE_HEARTBEAT;

            if((scanner.end - scanner.begin) == 12 )
            {
                heart->header = NULL;
                break;
            }

            pj_scan_get_until_ch(&scanner, ':', &out);
            pj_scan_get_char(&scanner);
            group_pool = pjsua_pool_create("heart_beat_pool", 256, 256);
            heart->header = pj_pool_alloc(group_pool, sizeof(heart_beat_group_info));
            heart->header->own_pool = group_pool;
            multicast_parse_heartbeat(&scanner, heart->header);
            if((scanner.end - scanner.curptr) <= 2)
                break;
            ichar= pj_scan_get_char(&scanner);
            while(ichar == ',')
            {
                heart_beat_group_info *heart_group_info = NULL;
                heart_group_info = pj_pool_alloc(group_pool, sizeof(heart_beat_group_info));
                multicast_parse_heartbeat(&scanner, heart_group_info);
                pj_list_push_back(&heart->header, heart_group_info);
                if((scanner.end - scanner.curptr) <= 2)
                    break;
                else
                    ichar = pj_scan_get_char(&scanner);
            }
            if(ichar == '\r' || ichar == '\n')
                break;

        }
        else
        {
            multicast_info.type = -1;
            pj_scan_skip_line(&scanner);
        }
    }
    rdata->multicast_info = multicast_info;
    return PJ_SUCCESS;
}

#endif



