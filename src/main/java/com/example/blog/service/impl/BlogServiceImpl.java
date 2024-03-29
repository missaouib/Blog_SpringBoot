package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.bean.Blog;
import com.example.blog.entity.query.QueryBlog;
import com.example.blog.entity.result.Result;
import com.example.blog.service.BlogService;
import com.example.blog.mapper.BlogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author 24933
* @description 针对表【t_blog】的数据库操作Service实现
* @createDate 2022-05-25 15:59:45
*/
@Service(value = "blogService")
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

	@Resource(name = "blogMapper")
	private BlogMapper blogMapper;

	/**
	 * 查询
	 * @param queryBlog 查询条件类
	 */
	@Override
	public Result getBlogPage(QueryBlog queryBlog) {
		Page<Blog> blogPage = new Page<>(queryBlog.getPageNum(),queryBlog.getPageSize());
		IPage<Blog> page = blogMapper.selectBlogPage(blogPage,queryBlog);
		return Result.success(page);
	}

	/**
	 * 查询指定博文
	 * @param id 博文id
	 */
	@Override
	public Result getBlogById(int id) {
		LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Blog::getDeleted,false)
				.eq(Blog::getPublish,true)
				.eq(Blog::getId,id);
		Blog blog = super.getOne(wrapper);
		return Result.success(blog);
	}

	/**
	 * 更新 view
	 * @param id 博文id
	 * @param count 新增访问量
	 */
	@Override
	public int updateView(int id, int count) {
		Blog blog = new Blog();
		blog.setId(id);
		blog.setViews(count);
		return blogMapper.update(blog);
	}

	/**
	 * 新增/修改 博文
	 * @param blog 博文内容
	 */
	@Override
	public Result saveBlog(Blog blog) {
		Integer id = blog.getId();
		if(id == null){
			//新增
			if (super.save(blog)) {
				return Result.success("新增成功",blog.getId());
			}
		}else {
			//修改
			if (super.updateById(blog)) {
				return Result.success("修改成功",null);
			}
		}
		return Result.fail("500","操作失败");
	}

	/**
	 * 永久删除 ---todo  待测试
	 * @param id 博文id
	 *
	 */
	@Override
	public Result delete(int id) {
		if (super.removeById(id)) {
			return Result.success("删除成功");
		}
		return Result.fail("500","删除失败");
	}

	/**
	 * 逻辑删除 ---todo  待测试
	 * @param id 博文id
	 */
	@Override
	public Result delBlog(int id) {
		Blog blog = new Blog();
		blog.setId(id);
		blog.setDeleted(1);
		if (super.updateById(blog)) {
			return Result.success("删除成功");
		}
		return Result.fail("500","删除失败");
	}
}




