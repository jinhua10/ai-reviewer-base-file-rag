/**
 * 愿望单 API 模块 (Wish List API Module)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import { request } from '../index'

const wishApi = {
  /**
   * 获取愿望单列表 (Get wish list)
   */
  getList(params) {
    return request.get('/wishes', params)
  },

  /**
   * 提交愿望 (Submit wish)
   */
  submit(data) {
    return request.post('/wishes', data)
  },

  /**
   * 投票 (Vote for wish)
   */
  vote(wishId) {
    return request.post(`/wishes/${wishId}/vote`)
  },

  /**
   * 取消投票 (Cancel vote)
   */
  cancelVote(wishId) {
    return request.delete(`/wishes/${wishId}/vote`)
  },

  /**
   * 获取排行榜 (Get ranking)
   */
  getRanking(params) {
    return request.get('/wishes/ranking', params)
  },

  /**
   * 获取我的愿望 (Get my wishes)
   */
  getMyWishes(params) {
    return request.get('/wishes/my', params)
  },
}

export default wishApi

