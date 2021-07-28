package com.bjpowernode.crm.workbench.service.impl;

import com.bjpowernode.crm.utils.DateTimeUtil;
import com.bjpowernode.crm.utils.SqlSessionUtil;
import com.bjpowernode.crm.utils.UUIDUtil;
import com.bjpowernode.crm.workbench.dao.*;
import com.bjpowernode.crm.workbench.domain.*;
import com.bjpowernode.crm.workbench.service.ClueService;

import java.util.List;

/**
 * @author huangyb
 * @create 2021-07-19 23:21
 */
public class ClueServiceImpl implements ClueService {

    private ClueDao clueDao = SqlSessionUtil.getSqlSession().getMapper(ClueDao.class);
    private ClueActivityRelationDao clueActivityRelationDao = SqlSessionUtil.getSqlSession().getMapper(ClueActivityRelationDao.class);
    private ClueRemarkDao clueRemarkDao = SqlSessionUtil.getSqlSession().getMapper(ClueRemarkDao.class);

    private CustomerDao customerDao = SqlSessionUtil.getSqlSession().getMapper(CustomerDao.class);
    private CustomerRemarkDao customerRemarkDao = SqlSessionUtil.getSqlSession().getMapper(CustomerRemarkDao.class);

    private ContactsDao contactsDao = SqlSessionUtil.getSqlSession().getMapper(ContactsDao.class);
    private ContactsRemarkDao contactsRemarkDao = SqlSessionUtil.getSqlSession().getMapper(ContactsRemarkDao.class);
    private ContactsActivityRelationDao contactsActivityRelationDao = SqlSessionUtil.getSqlSession().getMapper(ContactsActivityRelationDao.class);

    private TranDao tranDao = SqlSessionUtil.getSqlSession().getMapper(TranDao.class);
    private TranHistoryDao tranHistoryDao = SqlSessionUtil.getSqlSession().getMapper(TranHistoryDao.class);



    @Override
    public boolean save(Clue c) {
        boolean flag = true;

        int count = clueDao.save(c);

        if(count!=1){
            flag=false;
        }
        return flag;
    }

    @Override
    public Clue detail(String id) {

        Clue c = clueDao.detail(id);

        return c;
    }


    @Override
    public boolean unbund(String id) {

        boolean flag = true;

        int count = clueActivityRelationDao.unbund(id);

        if(count!=1){

            flag = false;

        }

        return flag;


    }

    @Override
    public boolean bund(String cid, String[] aids) {
        for(String aid : aids) {
            ClueActivityRelation car = new ClueActivityRelation();
            car.setId(UUIDUtil.getUUID());
            car.setActivityId(aid);
            car.setClueId(cid);
            System.out.println(car.toString());
            if (clueActivityRelationDao.bund(car) != 1) {
                return false;
            }
        }
        return true;
    }

    @Override

    public boolean convert(String clueId, Tran t, String createBy) {

        String createTime = DateTimeUtil.getSysTime();

        boolean flag = true;

        //(1) 通过线索id获取线索对象（线索对象中封装了线索信息）
        Clue c = clueDao.getById(clueId);

        //(2) 通过线索对象提取客户信息，当客户不存在的时候，新建客户（根据公司的名称精确匹配，判断该客户是否存在！）
        String company = c.getCompany();
        Customer cus = customerDao.getCustomerByName(company);
        //如果cus为null，说明以前没有这个客户，需要新建一个
        if(cus==null) {
            cus = new Customer();
            cus.setId(UUIDUtil.getUUID());
            cus.setAddress(c.getAddress());
            cus.setWebsite(c.getWebsite());
            cus.setPhone(c.getPhone());
            cus.setOwner(c.getOwner());
            cus.setNextContactTime(c.getNextContactTime());
            cus.setName(company);
            cus.setDescription(c.getDescription());
            cus.setCreateTime(createTime);
            cus.setCreateBy(createBy);
            cus.setContactSummary(c.getContactSummary());
            //添加客户
            int count1 = customerDao.save(cus);
            if(count1!=1){
                flag = false;
            }

        }

        //--------------------------------------------------------------------------
        //经过第二步处理后，客户的信息我们已经拥有了，将来在处理其他表的时候，如果要使用到客户的id
        //直接使用cus.getId();
        //--------------------------------------------------------------------------

        //(3)通过线索对象提取联系人信息，保存联系人
        Contacts con = new Contacts();
        con.setId(UUIDUtil.getUUID());
        con.setSource(c.getSource());
        con.setOwner(c.getOwner());
        con.setNextContactTime(c.getNextContactTime());
        con.setMphone(c.getMphone());
        con.setJob(c.getJob());
        con.setFullname(c.getFullname());
        con.setEmail(c.getEmail());
        con.setDescription(c.getDescription());
        con.setCustomerId(cus.getId());
        con.setCreateTime(createTime);
        con.setCreateBy(createBy);
        con.setContactSummary(c.getContactSummary());
        con.setAppellation(c.getAppellation());
        con.setAddress(c.getAddress());
        //添加联系人
        int count2 = contactsDao.save(con);
        if(count2!=1){
            flag = false;
        }

        //--------------------------------------------------------------------------
        //经过第三步处理后，联系人的信息我们已经拥有了，将来在处理其他表的时候，如果要使用到联系人的id
        //直接使用con.getId();
        //--------------------------------------------------------------------------

        //(4) 线索备注转换到客户备注以及联系人备注
        //查询出与该线索关联的备注信息列表
        List<ClueRemark> clueRemarkList = clueRemarkDao.getListByClueId(clueId);
        //取出每一条线索的备注
        for(ClueRemark clueRemark : clueRemarkList){

            //取出备注信息（主要转换到客户备注和联系人备注的就是这个备注信息）
            String noteContent = clueRemark.getNoteContent();

            //创建客户备注对象，添加客户备注
            CustomerRemark customerRemark = new CustomerRemark();
            customerRemark.setId(UUIDUtil.getUUID());
            customerRemark.setCreateBy(createBy);
            customerRemark.setCreateTime(createTime);
            customerRemark.setCustomerId(cus.getId());
            customerRemark.setEditFlag("0");
            customerRemark.setNoteContent(noteContent);
            int count3 = customerRemarkDao.save(customerRemark);
            if(count3!=1){
                flag = false;
            }

            //创建联系人备注对象，添加联系人
            ContactsRemark contactsRemark = new ContactsRemark();
            contactsRemark.setId(UUIDUtil.getUUID());
            contactsRemark.setCreateBy(createBy);
            contactsRemark.setCreateTime(createTime);
            contactsRemark.setContactsId(con.getId());
            contactsRemark.setEditFlag("0");
            contactsRemark.setNoteContent(noteContent);
            int count4 = contactsRemarkDao.save(contactsRemark);
            if(count4!=1){
                flag = false;
            }

        }
        return flag;
    }
}
