#ifndef __PTT_EXTENSION_H__
#define __PTT_EXTENSION_H__


PJ_BEGIN_DECL


typedef enum e_msg_action
{
    ACTION_GET_MEMBER,

    ACTION_APPLY_PTT_RIGHT,

    ACTION_RELEASE_PT_RIGHT,

    ACTION_CANCEL_PTT_RIGHT,

    ACTION_GET_GRP_NUMBER,

    MAX_ACTION_PTT

}msg_action;


typedef enum
{
    PTT_STATUS_UNINIT,

    PTT_STATUS_INIT,

    PTT_STATUS_FREE,

    PTT_STATUS_BUSY,

    PTT_STATUS_WAITING,

    PTT_STATUS_REJECTED,

    PTT_STATUS_CANCELED,

    PTT_STATUS_FORBIDDEN,

    MAX_PTT_STATUS

}ptt_status;




typedef struct ptt_group_info
{
    PJ_DECL_LIST_MEMBER(struct ptt_group_info);

    /** group name. */
    pj_str_t name;

    /** group number. */
    pj_str_t number;

    /** group level. */
    pj_uint8_t level;

    /** client to server:-1~3600 disabled?. */
    int report_heartbeat;

    /** server to client:0~3600. */
    int update_heartbeat;

    /** multicast address for client to listen. */
    pj_str_t mcast_address;

    /** multicast port. */
    unsigned port;

    /** construct by pjmedia_transport_udp_create.
     destruct by pjmedia_transport_close. */
    pjmedia_transport *transport;

    /** create before alloc, pj_list_erase to release. */
    pj_pool_t *own_pool;

}ptt_group_info;


typedef struct signal_info
{
    /** multicast address for client to listen. */
    pj_str_t mcast_address;

    /** multicast port. */
    unsigned port;

    /** ptt signal transport identification. */
    pjsua_transport_id transport_id;

}signal_info;


typedef struct reg_content
{
    /** compare with emergency. */
    ptt_group_info *general_group;

    /** maybe NULL. */
    ptt_group_info emergency_group;

    signal_info signal_address;

}reg_content;


typedef struct PTT_info
{
    pj_pool_t *snd_pool;/**< Sound's private pool.*/

    pj_timer_entry signal_timer;

    reg_content group_info;

}PTT_info;




/*
 * init
 * set global default value
 */
int ptt_init(void);

/*
 * release memory
 * clear global value
 */
int ptt_destroy(void);

/*
 * get ptt state
 * return value: ptt state
 */
ptt_status ptt_get_state(void);

/*
 * get destination number
 * arg: digit number
 */
void get_dest_number(pj_str_t *number);

/*
 * judge the number is group number or not
 * return value: true or false
 */
pj_bool_t group_number_compare(char *number);

/*
 * listen media multicast
 * open audio device
 */
pj_status_t ptt_open_media(void);

/*
 * leave media multicast
 * close audio device
 */
pj_status_t ptt_close_media(void);

/*
 * parse the content from the 200 ok message
 * when register to the server
 */
int ptt_reg200ok_content_parse(char * content, unsigned len);

/*
 * parse the content from the 200 ok message
 * when get group member with info
 */
int ptt_info200ok_content_parse(char * content, unsigned len);

/*
 * parse the content from the Message message
 * when its group changed
 */
int ptt_message_content_parse(char * content, unsigned len);

pj_status_t manage_ptt_group(pj_uint16_t optname);



PJ_END_DECL


#endif
