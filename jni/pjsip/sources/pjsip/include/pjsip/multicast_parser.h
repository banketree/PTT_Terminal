#ifndef __MULTICAST_PARSER_H__
#define __MULTICAST_PARSER_H__

#include <pjsip/sip_types.h>

PJ_BEGIN_DECL

typedef enum
{
    TYPE_HEARTBEAT,

    TYPE_INFO,

    TYPE_INVITE,

    TYPE_BYE,

    TYPE_MEMBER_STATE,

    TYPE_MAX

}MULTICAST_TYPE;

typedef struct multicast_INFO
{
    pj_str_t group_num;

    unsigned call_id;

    pj_str_t start_time;

    /** 1:idle; 2:busy. */
    unsigned char status;

    /** exist when status is 2. */
    pj_str_t speaker;

    /** exist when status is 2. */
    pj_str_t spk_start_time;

}multicast_INFO;

typedef struct heart_beat_group_info
{
    PJ_DECL_LIST_MEMBER(struct heart_beat_group_info);
    pj_str_t group_num;
    pj_pool_t * own_pool;
}heart_beat_group_info;


typedef struct multicast_HEART
{
    heart_beat_group_info *header;
    
}multicast_HEART;


typedef struct multicast_INVITE
{
    pj_str_t group_num;

    /** current right, may be NULL **/
    pj_str_t speaker;
#ifdef PJSIP_EXTEND_ZED
    /** 0~10000. **/
    unsigned call_id;
#endif

#ifdef PJSIP_EXTEND_ZZY
    pj_str_t call_id;
#endif
    /** time when group open. */
    pj_str_t start_time;

    /** may not exist. */
    pj_uint8_t codec;


}multicast_INVITE;

typedef struct multicast_BYE
{
    pj_str_t group_num;

#ifdef PJSIP_EXTEND_ZED
    /** 0~10000. **/
    unsigned call_id;
#endif
#ifdef PJSIP_EXTEND_ZZY
    pj_str_t call_id;
#endif

    /** time when group open. */
    pj_str_t start_time;

}multicast_BYE;

#ifdef PJSIP_EXTEND_ZZY
typedef struct multicast_MemberState
{
    pj_str_t num;

    pj_str_t name;

    unsigned status;

}multicast_MemberState;
#endif

typedef struct multicast_signal
{
    MULTICAST_TYPE type;

    union
    {
#ifdef PJSIP_EXTEND_ZED
        /** heart beat in INFO, send from MDS, 0~10000. */
        unsigned heart_beat;
#endif
#ifdef PJSIP_EXTEND_ZZY
        multicast_HEART heart_beat;
#endif

        /** INFO no heart beat. */
        multicast_INFO info;

        /** m-INVITE. */
        multicast_INVITE inv;

        /** m-BYE. */
        multicast_BYE bye;

#ifdef PJSIP_EXTEND_ZZY
        multicast_MemberState member_state;
#endif

    }multi;

}multicast_signal;

/**
 * Parse a packet buffer use part of rdata.
 *
 * This function is normally called by the transport layer.
 *
 * @param buf		The input buffer, which MUST be NULL terminated.
 * @param size		The length of the string (not counting NULL terminator).
 * @param rdata     use it when parse.
 *
 * @return          PJ_SUCCESS or error.
 */
int multicast_parse_rdata(char *buf, pj_size_t size, pjsip_rx_data *rdata);



PJ_END_DECL


#endif
